package com.jam.dentsu.neuroapi_example

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.android.synthetic.main.activity_main.*
import neuronicle.EEGGrpc
import neuronicle.NeuroNicleProto
import java.io.Console
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity(), neuroNicleService.Companion.NNListener {

    lateinit var mp0: MediaPlayer

    private var ch1RawArray: MutableList<Int> = IntArray(250){0}.toMutableList()
    private var ch2RawArray: MutableList<Int> = IntArray(250){0}.toMutableList()

    private val clientId = "sd_kogaku_2021"
    private val serverIP = "18.221.165.249"
    private val port = 80

    private val channel = ManagedChannelBuilder.forAddress(serverIP, port)
        .usePlaintext()
        .build()
    private val stub = EEGGrpc.newStub(channel)

    private val observer = stub.start(object : StreamObserver<NeuroNicleProto.ConversionReply> {
        override fun onNext(reply: NeuroNicleProto.ConversionReply) {
            //emotionLabel.text = "${reply.getDataOrDefault("Like",0)},${reply.getDataOrDefault("Interest",0)},${reply.getDataOrDefault("Concentration",0)},${reply.getDataOrDefault("Calmness",0)},${reply.getDataOrDefault("Stress",0)}"
            Log.d("observer","success")
        }

        override fun onError(t: Throwable) {
            Log.d("observer","error")
        }

        override fun onCompleted() {
            Log.d("observer","complete")
        }
    })

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.button)

        button.setOnClickListener{
            val intent = Intent(this, SubActivity::class.java)
            startActivity(intent)
            mp0.release()
        }

        val message = NeuroNicleProto.ConnectionRequest.newBuilder().setClientCode(clientId).build()

        stub.verify(message, object : StreamObserver<NeuroNicleProto.ConnectionReply> {
            override fun onNext(reply: NeuroNicleProto.ConnectionReply) {
                //verifyLabel.text = "verify:${reply.message}"
            }
            override fun onError(t: Throwable) {}

            override fun onCompleted() {
                println("complete")
            }
        })

        neuroNicleService.onCreateApplication(applicationContext)

        //脳波計を起動する際にはこちらを使用する
        //脳波計がない時にはifをfalseに変更し、こちらを使用する
        Timer().schedule(0, 4) {
            runOnUiThread {
                GetSimData()
            }
        }
        neuroNicleService.instance.isConnected = true
        neuroNicleService.instance.isFitting = true

        mp0= MediaPlayer.create(this,R.raw.illumination)
        mp0.isLooping=true

        mp0.start()
    }


    override fun onDestroy() {
        neuroNicleService.instance.isDestroyed=true
        val message = NeuroNicleProto.FinishRequest.newBuilder().setClientCode(clientId).build()
        stub.finishConnection(message, object : StreamObserver<NeuroNicleProto.FinishReply> {
            override fun onNext(reply: NeuroNicleProto.FinishReply) {
                println(reply.ok)
            }
            override fun onError(t: Throwable) {}

            override fun onCompleted() {
                println("complete")
            }
        })
        //mp0.release()
        super.onDestroy()
    }

    private var dataCount = 1;
    override fun onDataReceived(ch1: Int, ch2: Int) {
        ch1RawArray.add(ch1)
        ch2RawArray.add(ch2)
        ch1RawArray.removeAt(0)
        ch2RawArray.removeAt(0)
        if(dataCount==250){
            val message = NeuroNicleProto.ConversionRequest.newBuilder().setClientCode(clientId).addAllCh1(ch1RawArray).addAllCh2(ch2RawArray).build()
            observer.onNext(message)
            dataCount=0
        }
        dataCount+=1
    }

    private fun GetSimData(){
        val ch1 = (0..1000).random()+(9000..10000).random()
        val ch2 = (0..1000).random()+(9000..10000).random()
        ch1RawArray.add(ch1)
        ch2RawArray.add(ch2)
        ch1RawArray.removeAt(0)
        ch2RawArray.removeAt(0)
        if(dataCount==250){
            val message = NeuroNicleProto.ConversionRequest.newBuilder().setClientCode(clientId).addAllCh1(ch1RawArray).addAllCh2(ch2RawArray).build()
            observer.onNext(message)
            dataCount=0
        }
        dataCount+=1
    }
}

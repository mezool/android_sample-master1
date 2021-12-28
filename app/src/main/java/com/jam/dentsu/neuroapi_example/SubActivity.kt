package com.jam.dentsu.neuroapi_example

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.android.synthetic.main.activity_sub.*
import neuronicle.EEGGrpc
import neuronicle.NeuroNicleProto
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule

class SubActivity : AppCompatActivity() {

    lateinit var mp1: MediaPlayer

    companion object {
        private const val TERM_MILLISECOND: Long = 1000
    }
    private val img = intArrayOf(R.drawable.img, R.drawable.img_1, R.drawable.img_2, R.drawable.img_3)
    private var num = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub)

        Toast.makeText(this, "約10秒後に画像が表示されますので、画像に注目してください。", Toast.LENGTH_LONG).show()

        val imageView: ImageView = findViewById(R.id.imageView)
        val resultView: TextView = findViewById(R.id.resultView)
        val likeView: TextView = findViewById(R.id.likeView)

        //経過時間を表示したい
        var time = 0L
        val dataFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        val handler = Handler()
        val timer = object : Runnable{
            override fun run() {
                time += TERM_MILLISECOND
                timeDisplay.text = dataFormat.format(time)
                handler.postDelayed(this, TERM_MILLISECOND)
            }
        }
        handler.post(timer)
        //経過時間は表示できた
        //初期値はFalse
        imageView.visibility = View.GONE
        //画像表示
        handler.postDelayed({
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(img[0])
            num ++
        }, 10000)

        handler.postDelayed({
            imageView.visibility = View.GONE
            num++
        }, 20000)

        handler.postDelayed({
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(img[1])
            num ++
        }, 30000)

        handler.postDelayed({
            imageView.visibility = View.GONE
            num ++
        }, 40000)

        handler.postDelayed({
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(img[2])
            num++
        }, 50000)

        handler.postDelayed({
            imageView.visibility = View.GONE
            num++
        }, 60000)

        handler.postDelayed({
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(img[3])
            num++
        }, 70000)

        handler.postDelayed({
            imageView.visibility = View.GONE
            num++
        }, 80000)

        handler.postDelayed({
            num++
            Log.d("number", "$num")
        },90000)

        Timer().schedule(0, 4) {
            runOnUiThread {
                GetSimData()
            }
        }
        //3,2,1タイマー
        mp1 = MediaPlayer.create(this,R.raw.zihou)
        mp1.isLooping=false
        var countBgm = 0
        Timer().schedule(7000,20000){
            countBgm ++
            if (countBgm <= 4){
                mp1.start()
            }else{
                mp1.release()
            }

        }
    }


    private var ch1RawArray: MutableList<Int> = IntArray(250){0}.toMutableList()
    private var ch2RawArray: MutableList<Int> = IntArray(250){0}.toMutableList()

    private val clientId = "sd_kogaku_2021"
    private val serverIP = "18.221.165.249"
    private val port = 80

    private val channel = ManagedChannelBuilder.forAddress(serverIP, port)
        .usePlaintext()
        .build()
    private val stub = EEGGrpc.newStub(channel)

    private var like = 0
    private var LikeList1 = mutableListOf<Int>()
    private var LikeList2 = mutableListOf<Int>()
    private var LikeList3 = mutableListOf<Int>()
    private var LikeList4 = mutableListOf<Int>()

    private val observer = stub.start(object : StreamObserver<NeuroNicleProto.ConversionReply> {
        override fun onNext(reply: NeuroNicleProto.ConversionReply) {
            like = reply.getDataOrDefault("Like",0)
            likeView.text = "Like : $like"
            if (num == 8) {
                imageView.visibility = View.VISIBLE
                val x1 = (LikeList1.drop(2).dropLast(2)).average()
                val x2 = (LikeList2.drop(2).dropLast(2)).average()
                val x3 = (LikeList3.drop(2).dropLast(2)).average()
                val x4 = (LikeList4.drop(2).dropLast(2)).average()
                Log.d("平均値", "$x1, $x2, $x3, $x4")
                if (x1 > x2 && x1 > x3 && x1 > x4) {
                    imageView.setImageResource(img[0])
                }
                if (x2 > x1 && x2 > x3 && x2 > x4) {
                    imageView.setImageResource(img[1])
                }
                if (x3 > x2 && x3 > x1 && x3 > x4) {
                    imageView.setImageResource(img[2])
                }
                if (x4 > x2 && x4 > x3 && x4 > x3) {
                    imageView.setImageResource(img[3])
                }
            }
            Log.d("observer","success")
        }

        override fun onError(t: Throwable) {
            Log.d("observer","error")
        }

        override fun onCompleted() {
            Log.d("observer","complete")
        }
    })

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
        super.onDestroy()
    }

    private var dataCount = 1;
    fun onDataReceived(ch1: Int, ch2: Int) {
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
            if(false) {
                val message = NeuroNicleProto.ConversionRequest.newBuilder().setClientCode(clientId).addAllCh1(ch1RawArray).addAllCh2(ch2RawArray).build()
                observer.onNext(message)
            } else {
                like = (20..80).random()+(-20..20).random()
                likeView.text = "Like : $like"
                //Log.d("好き度は", "$like")
                if (num == 0) { LikeList1.add(like) }
                if (num == 2) { LikeList2.add(like) }
                if (num == 4) { LikeList3.add(like) }
                if (num == 6) { LikeList4.add(like) }
                Log.d("ライクリスト", "$LikeList1,$LikeList2,$LikeList3,$LikeList4,")
            }
            dataCount=0
        }
        dataCount+=1
        //ライクリストが全てで揃ったら分岐して表示
        if (num == 8) {
            imageView.visibility = View.VISIBLE
            val x1 = (LikeList1.drop(2).dropLast(2)).average()
            val x2 = (LikeList2.drop(2).dropLast(2)).average()
            val x3 = (LikeList3.drop(2).dropLast(2)).average()
            val x4 = (LikeList4.drop(2).dropLast(2)).average()
            Log.d("平均値", "$x1, $x2, $x3, $x4")
            if (x1 > x2 && x1 > x3 && x1 > x4) {
                imageView.setImageResource(img[0])
            }
            if (x2 > x1 && x2 > x3 && x2 > x4) {
                imageView.setImageResource(img[1])
            }
            if (x3 > x2 && x3 > x1 && x3 > x4) {
                imageView.setImageResource(img[2])
            }
            if (x4 > x2 && x4 > x3 && x4 > x3) {
                imageView.setImageResource(img[3])
            }
        }
    }
}
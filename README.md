# neuroapi_example
neuronicle apiのクライアントサンプル  
  
private val clientID : DSJから発行されたclient idを指定  
private val serverIP : DSJから通知されたIPアドレス  
private val port : DSJから通知されたポート番号  
  
stub.verify : client idを送信し、通信状態をテストします  
stub.start : 脳波データを送信し、感性データを取得します。onNextでConversionRequest形式の脳波データを送信できます。  
stub.finishConnection : サーバに蓄積されているキャリブレーションデータをリセットします。  


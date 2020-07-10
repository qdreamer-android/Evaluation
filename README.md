爱互动 Android 口语评测
=

开发环境:
-
开发工具: Android Studio 4.0  
开发语言: Kotlin  
JDK版本: 1.8


使用说明:
-
#### 一. 导入依赖:
&emsp;&emsp;请去[爱互动官网](http://www.ihudongketang.com/) 下载 aar 依赖包;其中有"评测"和"通信"两个包; 如果只需要本地口语评测模块,仅需要下载评测的依赖包, 而需要用到互动管理系统时,还需要再导入通信的依赖包;

&emsp;&emsp;例如,将上面的提到的依赖包放在该项目根目录的 aar 目录下,项目更目录的 build.gradle 配置如下:
```groovy
allprojects {
    repositories {
        google()
        jcenter()

        maven {url 'file:' + rootProject.rootDir + '/aar/'}
    }
}
```  

&emsp;&emsp;再在 app 应用模块的 build.gradle 中添加如下依赖:
```groovy
android {
    defaultConfig {
        // ...
        ndk {
            abiFilters "armeabi-v7a"
        }
    }
}

dependencies {
    // 评测模块依赖
    implementation 'com.qdreamer:eval:1.0.0'
    // 互动系统 MQTT 通信依赖    
    implementation 'com.pwong:mqtt:1.0.0'
}
```

&emsp;&emsp;添加权限,打开项目的 AndroidManifest.xml 清单文件,添加如下权限
1. 口语评测需要用到录音机和文件读写,注意动态权限申请
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```
2. 如果用到通信模块,还需要添加网络相关的权限
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<!-- 前台服务权限用于通信 Service 挂在前台通知,提高存活率,并且由于 Android 8 以上对后台服务的限制,只能跑在前台. -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

#### 二. 集成评测:
**1. 在 Activity 中定义全局变量 QLocalEvaluation**    
```kotlin
private val mEvaluation: QLocalEvaluation by lazy {
    QLocalEvaluation(this).apply {
        // 评测引擎错误回调
        addOnEvaluationErrorListener(this@Activity)
        // 评测开始结束和说话状态回调
        addOnEvaluationInfoListener(this@Activity)
        // 评测引擎初始化完成回调
        addOnInitListener(this@Activity)
    }
}
```
**2. 评测引擎异常回调** 
```kotlin
/**
 * 评测错误消息回调
 * [type] 异常类型
 * [msg] 错误描述
 */
override fun onError(type: Int, msg: String) {
}
```
**3. 评测引擎初始化完成回调**  
```kotlin
override fun onInitOver() {
}
```
**4. 评测引擎评测数据回调**  
```kotlin
/**
 * 评测开始回调
 * [isWord] true 表示评测单词, false 表示评测句子
 * [content] 评测的文本内容
 */
override fun onEvalStart(isWord: Boolean, content: String) {
}

/**
 * 评测结束回调
 * [isWord] true 表示评测单词, false 表示评测句子
 * [evalResult] 评测结果json数据, 当用户未说话时,评测结果为空
 * 单词评测解析为 EngWordEvalBean, 句子评测解析为 EngSentEvalBean;
 */
override fun onEvalResult(isWord: Boolean, evalResult: String?) {
}

/**
 * 评测引擎检测到说话状态改变回调
 * [isSpeak] true 表示说话开始, false 表示停止说话
 */
override fun onSpeakChange(isSpeak: Boolean) {
}
```
**5. 初始化评测引擎**  
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mEvaluation.init(APP_ID, APP_KEY)
}
```
**6. 开始评测**  
&emsp;&emsp;调用 startEvaluation() 方法,传入需要评测的英文文本开始口语评测
```kotlin
mEvaluation.startEvaluation(evalWord)
```
**7. 结束评测**  
&emsp;&emsp;只有在开始评测之后,调用 stopEvaluation() 方法,才会触发评测结果返回,如果用户有开口评测,返回对应json数据,用户未开口评测,返回的数据为null;
```kotlin
mEvaluation.stopEvaluation()
```
**2.8. 取消评测**  
&emsp;&emsp;开始评测之后,可以调用 cancelEvaluation() 方法,取消本次评测,此时评测数据直接丢弃,不会有评测结果返回;
```kotlin
mEvaluation.cancelEvaluation()
```
**2.9. 销毁评测引擎**  
&emsp;&emsp;在退出 Activity 时,请调用如下方法释放评测引擎
```kotlin
mEvaluation.cancelEvaluation()
mEvaluation.release()
```

[单词评测结果](eval_result_word.png)

[句子评测结果](eval_result_sent.png)


#### 三. MQTT 通信服务的使用:
**1. 创建 PahoHelper 实例**
```kotlin
val options = MqttOptions(
    serviceUri = "MQTT 服务器地址",
    clientId = "客户端ID,注意唯一性",
    subTopic = "订阅的 MQTT 消息 Topic",
    pubTopic = "默认 MQTT 发布的 Topic",
    willTopic = "遗嘱消息Topic",
    username = "MQTT 服务器账号",
    password = "MQTT 服务器密码"
)

mPahoHelper = PahoHelper(this, options)
// 设置 MQTT 消息监听
mPahoHelper.addOnMsgListener(this@Activity)
// 设置 MQTT 连接状态监听
mPahoHelper.addOnStatusChangeListener(this@Activity)
```
**2. MQTT 连接状态改变回调**
```kotlin
/**
 * MQTT 已做断线自动重连,在状态变化时,只需要提示下用户注意即可
 * [state] PahoStatus.SUCCESS 连接成功; PahoStatus.FAILURE 连接失败; PahoStatus.LOST 连接中断
 * [throwable] 连接失败或连接中断时的异常信息
 */
override fun onChange(state: PahoStatus, throwable: Throwable?) {
}
```
**3. MQTT 消息回调**
```kotlin
/**
 * MQTT 订阅的消息回调, 该回调在子线程中
 * [topic] 订阅的 MQTT Topic
 * [message] 订阅的消息内容
 */
override fun onSubMessage(topic: String, message: String) {
}
/**
 * MQTT 发布的消息回调, 用来确认 MQTT 消息是否发送成功
 * [message] 发送成功的消息内容
 */
override fun onPubMessage(message: String) {
}
```

**4. 发布 MQTT 消息**
```kotlin
/**
 * 发送 MQTT 消息, 如果不传 topic, 将使用 初始化时的 options 中定义的默认 pubTopic;
 * 如果消息发送成功,将会触发上面的 onPubMessage() 回调
 */
mPahoHelper.pubMessage(topic, message)
```

**5. 断开 MQTT 连接**  
&emsp;&emsp;退出 Activity 时,需要在 onDestroy() 中断开 MQTT 的连接
```kotlin
mPahoHelper.disConnect()
```
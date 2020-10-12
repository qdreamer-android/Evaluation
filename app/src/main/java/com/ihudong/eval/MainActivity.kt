package com.ihudong.eval

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.pwong.mqtt.OnPahoMsgListener
import com.pwong.mqtt.OnPahoStatusChangeListener
import com.pwong.mqtt.PahoHelper
import com.pwong.mqtt.PahoStatus
import com.qdreamer.QLocalEvaluation
import com.qdreamer.qvoice.OnEvaluationErrorListener
import com.qdreamer.qvoice.OnEvaluationInfoListener
import com.qdreamer.qvoice.OnInitListener
import com.qdreamer.utils.JsonHelper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions
import java.util.concurrent.TimeUnit


@RuntimePermissions
class MainActivity : AppCompatActivity(), OnPahoMsgListener, OnPahoStatusChangeListener,
    OnEvaluationErrorListener, OnEvaluationInfoListener, OnInitListener, View.OnClickListener {

    private companion object {
        private const val EVAL_APP_ID = "983f61d6-0103-11ea-b526-00163e13c8a2"
        private const val EVAL_APP_KEY = "36b2e346-07fb-32e3-9d36-bff9a71890b4"
    }

    private lateinit var mPahoHelper: PahoHelper

    private var mDisposable: Disposable? = null

    private var mEvalDisposable: Disposable? = null

    private val mEvaluation: QLocalEvaluation by lazy {
        QLocalEvaluation(this).apply {
            addOnEvaluationErrorListener(this@MainActivity)
            addOnEvaluationInfoListener(this@MainActivity)
            addOnInitListener(this@MainActivity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnEval?.setOnClickListener(this)
        btnEval?.isEnabled = false

        initEngineWithPermissionCheck()
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun initEngine() {
        mDisposable = Observable.create<Any> {
            mEvaluation.init(EVAL_APP_ID, EVAL_APP_KEY)
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {

                }
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onDenied() {
        Toast.makeText(this, "无法初始化引擎，没有文件存储和录音权限", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        mDisposable?.dispose()
        mEvalDisposable?.dispose()
        mEvaluation?.release()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnEval -> {
                val content = editEval.text?.toString()
                if (content.isNullOrBlank()) {
                    Toast.makeText(this, "请先输入要评测的文本", Toast.LENGTH_SHORT).show()
                } else {
                    v.isEnabled = false
                    txtEvalResult?.text = ""
                    mEvaluation.startEvaluation(content)
                    startEvaluationDownTimer(if (content.contains(" ")) 6 else 3)
                }
            }
        }
    }

    private fun startEvaluationDownTimer(time: Long) {
        mEvalDisposable?.dispose()
        mEvalDisposable = Observable.timer(time, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    mEvaluation.stopEvaluation()
                    btnEval?.isEnabled = true
                }, {
                    mEvaluation.stopEvaluation()
                    btnEval?.isEnabled = true
                })
    }

    /**
     * MQTT 通信连接状态改变回调
     */
    override fun onChange(state: PahoStatus, throwable: Throwable?) {

    }

    /**
     * 接收到的 MQTT 订阅消息
     * [topic] 订阅的 MQTT 主题
     * [message] 收到的 MQTT 消息
     */
    override fun onSubMessage(topic: String, message: String) {

    }

    /**
     * MQTT 发送消息成功回调
     */
    override fun onPubMessage(message: String) {

    }

    /**
     * 评测引擎初始化完成
     */
    override fun onInitOver() {
        btnEval?.isEnabled = true
    }

    /**
     * 评测引擎异常回调
     */
    override fun onError(type: Int, msg: String) {

    }

    /**
     * 评测开始
     * [isWord] true 单词口语评测  false 句子口语评测
     * [content] 评测文本
     */
    override fun onEvalStart(isWord: Boolean, content: String) {
        Log.i("Evaluation", "onEvalStart >>> $isWord  >>>> $content")
    }

    /**
     * 评测结果返回
     * [isWord] true 单词口语评测  false 句子口语评测
     * [evalResult] 口语评测结果,未检测到用户说话时为空
     * 单词评测结果解析为 com.qdreamer.entity.EngWordEvalBean
     * 句子评测结果解析为 com.qdreamer.entity.EngSentEvalBean
     */
    override fun onEvalResult(isWord: Boolean, evalResult: String?) {
        Log.i("Evaluation", "onEvalResult >>> $isWord  >>>> $evalResult")
        txtEvalResult?.text = if (evalResult.isNullOrEmpty()) {
            "无评测结果"
        } else {
            val total = JsonHelper.getJsonString(evalResult, "total")
            if (total.isNotEmpty()) {
                Toast.makeText(this, total, Toast.LENGTH_LONG).show()
            }
            evalResult
        }
    }

    /**
     * 用户说话状态改变回调
     * [isSpeak] 用户是否在说话
     */
    override fun onSpeakChange(isSpeak: Boolean) {
        Log.i("Evaluation", "onSpeakChange >>> $isSpeak")
    }

}
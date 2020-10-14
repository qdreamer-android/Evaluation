package com.ihudong.eval

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.qdreamer.QLocalEvaluation
import com.qdreamer.entity.EngSentEvalBean
import com.qdreamer.entity.EngWordEvalBean
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
class MainActivity : AppCompatActivity(), View.OnClickListener,
    OnEvaluationErrorListener, OnEvaluationInfoListener, OnInitListener {

    private companion object {
        private const val EVAL_APP_ID = "983f61d6-0103-11ea-b526-00163e13c8a2"
        private const val EVAL_APP_KEY = "36b2e346-07fb-32e3-9d36-bff9a71890b4"
    }

    private var mDisposable: Disposable? = null

    private var mEvalDisposable: Disposable? = null

    private val mEvalAdapter: EvalAdapter by lazy {
        EvalAdapter()
    }

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

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = mEvalAdapter

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
        mEvaluation.release()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnEval -> {
                val content = editEval.text?.toString()
                if (content.isNullOrBlank()) {
                    Toast.makeText(this, "请先输入要评测的文本", Toast.LENGTH_SHORT).show()
                } else {
                    v.isEnabled = false
                    mEvalAdapter.clearList()
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
        Log.i("Evaluation", "onEvalResult >>> $isWord  >>>> $evalResult >>>> ${Thread.currentThread().name}")
        if (evalResult.isNullOrEmpty()) {
            Toast.makeText(this, "无评测结果", Toast.LENGTH_LONG).show()
        } else {
            mEvalAdapter.clearList()
            if (isWord) {
                val evalBean = JsonHelper.fromJson(evalResult, EngWordEvalBean::class.java)
                Toast.makeText(this, "${evalBean.total}", Toast.LENGTH_LONG).show()
                if (!evalBean.scores.isNullOrEmpty()) {
                    val scoreBean = evalBean.scores[0]
                    mEvalAdapter.addEvalTxtScore(EvalTxtScore(scoreBean.charX, scoreBean.score))
                    if (!scoreBean.phone.isNullOrEmpty()) {
                        val list = scoreBean.phone.map {
                            EvalTxtScore(it.charX, it.score)
                        }
                        mEvalAdapter.addEvalTxtScore(list)
                    }
                }
            } else {
                val evalBean = JsonHelper.fromJson(evalResult, EngSentEvalBean::class.java)
                Toast.makeText(this, "${evalBean.total}", Toast.LENGTH_LONG).show()
                mEvalAdapter.addEvalTxtScore(EvalTxtScore(evalBean.sentence, evalBean.total))
                evalBean.scores?.map {
                    EvalTxtScore(it.charX, it.score)
                }?.let {
                    mEvalAdapter.addEvalTxtScore(it)
                }
            }
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
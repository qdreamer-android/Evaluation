package com.ihudong.eval

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView


/**
 * @Author QDreamer-java & IHuDong-java
 * @Date   14 十月, 2020, 星期三
 */
class EvalAdapter : RecyclerView.Adapter<EvalAdapter.EvalViewHolder>() {

    private val mItemList = ArrayList<EvalTxtScore>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvalViewHolder {
        val itemView: View = LayoutInflater.from(parent.context).inflate(R.layout.item_eval, parent, false)
        return EvalViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EvalViewHolder, position: Int) {
        val txtEvalPhonetic = holder.findView<TextView>(R.id.txtEvalPhonetic)
        val txtPhoneticScore = holder.findView<TextView>(R.id.txtPhoneticScore)
        txtEvalPhonetic.isSelected = true
        if (position == 0) {
            txtEvalPhonetic.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            txtEvalPhonetic.setTextColor(Color.parseColor("#000000"))
            txtPhoneticScore.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            txtPhoneticScore.setTextColor(Color.parseColor("#FF0000"))
        } else {
            txtEvalPhonetic.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
            txtEvalPhonetic.setTextColor(Color.parseColor("#333333"))
            txtPhoneticScore.typeface = Typeface.defaultFromStyle(Typeface.NORMAL)
            txtPhoneticScore.setTextColor(Color.parseColor("#BB2222"))
        }
        txtEvalPhonetic.text = mItemList[position].phonetic
        txtPhoneticScore.text = "${mItemList[position].score}"
    }

    override fun getItemCount(): Int {
        return mItemList.size
    }

    fun addEvalTxtScore(evalTxtScore: EvalTxtScore) {
        mItemList.add(evalTxtScore)
        notifyItemInserted(mItemList.size - 1)
    }

    fun addEvalTxtScore(evalTxtScoreList: List<EvalTxtScore>) {
        mItemList.addAll(evalTxtScoreList)
        notifyItemRangeInserted(mItemList.size - evalTxtScoreList.size, evalTxtScoreList.size)
    }

    fun clearList() {
        mItemList.clear()
        notifyDataSetChanged()
    }

    inner class EvalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun <T : View> findView(@IdRes resId: Int): T {
            return itemView.findViewById(resId)
        }
    }

}
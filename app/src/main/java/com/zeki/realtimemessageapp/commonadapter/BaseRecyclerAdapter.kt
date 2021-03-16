package com.zeki.realtimemessageapp.commonadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class BaseRecyclerAdapter<Bean, Binding : ViewDataBinding>
constructor(
    private val layoutRes: Int,
    private val onCellClick: ((Bean) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var baseList: List<Bean> = emptyList()

    class BaseSimpleViewHolder<Binding : ViewDataBinding>(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        val binding: Binding? by lazy {
            DataBindingUtil.bind<Binding>(itemView)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return BaseSimpleViewHolder<Binding>(
            LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        )
    }

    override fun getItemCount():Int{
        return baseList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as BaseSimpleViewHolder<Binding>
        holder.binding!!.root.setOnClickListener {
            onCellClick?.invoke(baseList[position])
        }
        bindData(holder.binding!!, holder, position)
    }

    abstract fun bindData(binding: Binding, holder: RecyclerView.ViewHolder, position: Int)

    open fun replaceData(newList: List<Bean>?) {
        baseList = newList ?: emptyList()
        notifyDataSetChanged()
    }

    open fun replaceDataWithDiff(newList: List<Bean>) {
        if (baseList.isEmpty()) {
            baseList = newList
            notifyDataSetChanged()
        } else {
            if (newList.isNotEmpty()) {
                val diffResult = DiffUtil.calculateDiff(
                    SingleBeanDiffCallBack(
                        baseList,
                        newList
                    ),
                    true
                )
                baseList = newList
                diffResult.dispatchUpdatesTo(this)
            } else {
                baseList = newList
                notifyDataSetChanged()
            }
        }
    }

}

class SingleBeanDiffCallBack<Bean>(
    val oldDatas: List<Bean>,
    val newDatas: List<Bean>
) : DiffUtil.Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return true
    }

    override fun getOldListSize(): Int {
        return oldDatas.size
    }

    override fun getNewListSize(): Int {
        return newDatas.size
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldData = oldDatas[oldItemPosition]
        val newData = newDatas[newItemPosition]
        val a = oldData == newData
        return oldData == newData
    }
}

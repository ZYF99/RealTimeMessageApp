package com.zeki.realtimemessageapp.ui.home

import androidx.recyclerview.widget.RecyclerView
import com.zeki.realtimemessageapp.R
import com.zeki.realtimemessageapp.commonadapter.BaseRecyclerAdapter
import com.zeki.realtimemessageapp.databinding.ItemRtcUserBinding
import com.zeki.realtimemessageapp.model.RTCUser

class RTCRecyclerAdapter(
    private val onCallButtonClick: ((RTCUser) -> Unit)? = null
) : BaseRecyclerAdapter<RTCUser, ItemRtcUserBinding>(
    R.layout.item_rtc_user
) {
    override fun bindData(binding: ItemRtcUserBinding, holder: RecyclerView.ViewHolder, position: Int) {
        binding.rtcUser = baseList[position]
        binding.btnCall.setOnClickListener {
            onCallButtonClick?.invoke(baseList[position])
        }
    }
}
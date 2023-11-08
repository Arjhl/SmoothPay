package com.example.smoothpay

import android.content.DialogInterface.OnClickListener
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import androidx.recyclerview.widget.RecyclerView
import com.example.smoothpay.databinding.SingleItemBinding

private var TAG = "RvMerchantAdapter"
class RvMerchantsAdapter(private var merchantsList : List<Merchants> ) : RecyclerView.Adapter<RvMerchantsAdapter.ViewHolder>(){
    inner class ViewHolder (val binding: SingleItemBinding) : RecyclerView.ViewHolder(binding.root){

    }

    var onItemClick:((Merchants)->Unit)? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SingleItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)

        return ViewHolder(binding)
    }

    override fun getItemCount()=  merchantsList.size



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var item = merchantsList[position]
       with(holder){
           with(merchantsList[position]){
               binding.tvUpiid.text = this.merchantName
               binding.tvLocation.text = this.upiId.split("=")[1].split("&")[0]
           }
           holder.binding.itemConstraint.setOnClickListener{
                Log.i(TAG,"position $position")
                onItemClick?.invoke(merchantsList[position])
           }
       }
    }

}


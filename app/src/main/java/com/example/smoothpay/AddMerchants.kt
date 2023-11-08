package com.example.smoothpay

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.smoothpay.databinding.ActivityAddMerchantsBinding
import com.example.smoothpay.databinding.SingleItemBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.content.Intent
import android.net.Uri

class AddMerchants : AppCompatActivity() {

    private lateinit var binding:ActivityAddMerchantsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMerchantsBinding.inflate(layoutInflater)

        var msg = intent.extras?.getString("upiID")


        binding.tvUpiID.text = msg.toString().split("=")[1].split("&")[0]

        setContentView(binding.root)


        binding.btnAddMerchant.setOnClickListener(){
            var name = binding.tvMerchantName.text.toString()
            var upiId = msg.toString()
            var result = Intent()
            result.putExtra("merchantName",name)
            result.putExtra("upiId",upiId)
            setResult(Activity.RESULT_OK,result)
            finish()

        }
    }


}
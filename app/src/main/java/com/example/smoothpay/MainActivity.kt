package com.example.smoothpay


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smoothpay.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanCustomCode
import io.github.g00fy2.quickie.config.ScannerConfig
import kotlinx.coroutines.runBlocking

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable


data class Merchants(
    val merchantName: String,
    val upiId: String,
    val latitude: String,
    val longitude: String
) : Serializable


@Suppress("DEPRECATION")
class MainActivity() : AppCompatActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var binding: ActivityMainBinding
    private lateinit var rvMerchantsAdapter: RvMerchantsAdapter
    private lateinit var merchantList: MutableList<Merchants>
    //    private var FILENAME = "merchants.txt"

    private var latitude = "0"
    private var longitude = "0"


    fun onLocationFetch(){
        getMerchants()
        rvMerchantsAdapter.notifyDataSetChanged()
    }

    private val addMerchantLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.i(TAG, "result ig ${result}")
            if (result.resultCode == RESULT_OK) {
                val newName = result.data?.getStringExtra("merchantName").toString()
                val isDuplicate = merchantList.filter { it.merchantName == newName }
                if (isDuplicate.size > 0) {
                    Toast.makeText(this, "Merchant Name Should be Unique", Toast.LENGTH_LONG).show()
                } else {
                    val newUpiId = result.data?.getStringExtra("upiId").toString()
                    val newMerchant = Merchants(newName, newUpiId, latitude, longitude)

                    val last = if (merchantList.size == 0) 0 else merchantList.size
                    merchantList.add(last, newMerchant)
                    merchantList = sortMerchants(merchantList)
                    //add to a local file
                    File(filesDir.toString(), "merchants.txt")
                    ObjectOutputStream(
                        FileOutputStream(
                            File(
                                filesDir,
                                "merchants.txt"
                            )
                        )
                    ).use { it.writeObject(merchantList) }
                    rvMerchantsAdapter.notifyDataSetChanged()

                }
            }
        }


    private val scanQrCodeLauncher =
        registerForActivityResult(ScanCustomCode()) { result: QRResult ->
            // handle QRResult
            when (result) {
                is QRResult.QRSuccess -> {
                    result.content.rawValue
                    result.content.rawValue?.let { Log.i(TAG, it) }
                    addMerchantLauncher.launch(
                        Intent(this, AddMerchants::class.java).putExtra(
                            "upiID",
                            result.content.rawValue
                        )
                    )
                }

                else -> {
                    Log.i(TAG, "No result")
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        merchantList = mutableListOf()
        getMerchants()

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvMerchants.layoutManager = layoutManager

        rvMerchantsAdapter = RvMerchantsAdapter(merchantList)

        binding.rvMerchants.adapter = rvMerchantsAdapter




        rvMerchantsAdapter.onItemClick = {
            Log.i(TAG, "recycle button clicked $it")
            //intent should go to upi apps
            val url = it.upiId
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }

        binding.faAdd.setOnClickListener {
            scanQrCodeLauncher.launch(
                ScannerConfig.build {
                    setHapticSuccessFeedback(true) // enable (default) or disable haptic feedback when a barcode was detected
                    setShowTorchToggle(true) // show or hide (default) torch/flashlight toggle button
                    setShowCloseButton(true) // show or hide (default) close button
                }
            )
        }


    }

    private fun distance(lat1: String, lat2: String, long1: String, long2: String): Double {
        val r = 6371
        val p = Math.PI / 180

        val a =
            0.5 - Math.cos((lat2.toDouble() - lat1.toDouble()) * p) / 2 + Math.cos(lat1.toDouble() * p) * Math.cos(
                lat2.toDouble() * p
            ) *
                    (1 - Math.cos((long2.toDouble() - long1.toDouble()) * p)) / 2

        return 2 * r * Math.asin(Math.sqrt(a))

    }

    private fun sortMerchants(notSortedList: MutableList<Merchants>): MutableList<Merchants> {
        val distanceMerchantPair = mutableMapOf<String, Double>()

        for (item in notSortedList) {
            distanceMerchantPair[item.merchantName.toString()] =
                distance(latitude, item.latitude, longitude, item.longitude)
        }

        val sortedMap = distanceMerchantPair.toSortedMap()

        val result: MutableList<Merchants> = mutableListOf()
        for (m in sortedMap) {
            val element = notSortedList.filter { it.merchantName == m.key }[0]
            result.add(result.lastIndex + 1, element)
        }
        return result
    }

    private fun getMerchants() {

        var dummyList = mutableListOf<Merchants>()

        val path = filesDir.toString()
        val dataFile = File(path, "merchants.txt")
        if (!dataFile.exists()) {
            Log.i(TAG, "Files doesnt exist yet")
            File(filesDir.toString(), "merchants.txt")
//            dataFile.createNewFile()

            ObjectOutputStream(
                FileOutputStream(
                    File(
                        filesDir,
                        "merchants.txt"
                    )
                )
            ).use { it.writeObject(dummyList) }
        }
        ObjectInputStream(FileInputStream(dataFile)).use {
            dummyList = it.readObject() as MutableList<Merchants>
        }


        merchantList = sortMerchants(dummyList)

    }


    private fun getCurrentLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                //final latitude and longitude
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    Toast.makeText(this,"If location not enabled the sorting doesnt work",Toast.LENGTH_LONG).show()
                    return
                }
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        Toast.makeText(this, "NULL LOCATION", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Get Success", Toast.LENGTH_SHORT).show()
                        Log.i(TAG, "$location")
                        onLocationFetch()
                        latitude = location.latitude.toString()
                        longitude = location.longitude.toString()

                    }
                }

            } else {
                //setting open
                Toast.makeText(this, "Turn on Location", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            //request permission here
            requestPermission()
        }
    }


    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), PERMISSION_REQUEST_CODE
        )
    }

    companion object {
        private var TAG = "MAIN ACTIVITY"
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_LONG).show()
                getCurrentLocation()
            } else {
                Toast.makeText(applicationContext, "Denied", Toast.LENGTH_LONG).show()
            }
        }
    }

}


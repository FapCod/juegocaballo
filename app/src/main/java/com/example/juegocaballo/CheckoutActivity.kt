package com.example.juegocaballo



import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.ArrayList

class CheckoutActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CheckoutActivity"
        private const val BACKEND_URL = "https://stripe-horsegame.herokuapp.com"
    }
    private var level:Int? = 1;
    private lateinit var paymentIntentClientSecret: String
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var payButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)
        //obtener datos de la otra pestana
        val bundle = intent.extras;
        level = bundle?.getInt("level");
        if (level == null) level = 1
        // Hook up the pay button
        payButton = findViewById(R.id.payButton)
        payButton.setOnClickListener(::onPayClicked)
        payButton.isEnabled = false
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)
        fetchPaymentIntent()
    }

    private fun fetchPaymentIntent() {
        val url = "$BACKEND_URL/create-payment-intent"
        val amount = 100.0f;
        val payMap: MutableMap<String,Any> = HashMap();
        val itemMap: MutableMap<String,Any> = HashMap();
        val itemList: MutableList<Map<String,Any>> = ArrayList();
        payMap["currency"] ="usd";
        itemMap["id"]= "photo_suscription"
        itemMap["amount"] = amount;
        itemList.add(itemMap);
        payMap["items"]= itemList;
        val shoppingCartContent = Gson().toJson(payMap);
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = shoppingCartContent.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        OkHttpClient()
            .newCall(request)
            .enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showAlert("Failed to load data", "Error: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        showAlert("Failed to load page", "Error: $response")
                    } else {
                        val responseData = response.body?.string()
                        val responseJson = responseData?.let { JSONObject(it) } ?: JSONObject()
                        paymentIntentClientSecret = responseJson.getString("clientSecret")
                        runOnUiThread { payButton.isEnabled = true }
                        Log.i(TAG, "Retrieved PaymentIntent")
                    }
                }
            })
    }

    private fun showAlert(title: String, message: String? = null) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
            builder.setPositiveButton("Ok", null)
            builder.create().show()
        }
    }

    private fun showSnackBar(message: String,duration:Int) {
        val mySnackbar = Snackbar.make(findViewById(R.id.lyMain),message,duration);
        mySnackbar.show();

    }

    private fun onPayClicked(view: View) {
        val configuration = PaymentSheet.Configuration("Example, Inc.")
        // Present Payment Sheet
        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration)
    }

    private fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        when (paymentResult) {
            is PaymentSheetResult.Completed -> {
//                showSnackBar("Payment complete!",Snackbar.LENGTH_LONG);
                becamePremium();
            }
            is PaymentSheetResult.Canceled -> {
                showSnackBar("Payment canceled!",Snackbar.LENGTH_LONG);
            }
            is PaymentSheetResult.Failed -> {
                showSnackBar("Payment failed", Snackbar.LENGTH_LONG);
            }
        }
    }
    private fun becamePremium(){
        var sharedPreferences: SharedPreferences;
        sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        var editor = sharedPreferences.edit()
        editor.apply {
            putBoolean("PREMIUM",true)
            putInt("LEVEL",level!!)
        }.apply()

        val intent = Intent(this,MainActivity::class.java);
        startActivity(intent);
    }
}
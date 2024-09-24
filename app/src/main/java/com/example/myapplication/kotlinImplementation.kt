package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cloudipsp.android.Cloudipsp
import com.cloudipsp.android.CloudipspWebView
import com.cloudipsp.android.GooglePayCall
import com.cloudipsp.android.Order
import com.cloudipsp.android.Receipt

class MainActivityKotlin : AppCompatActivity(), View.OnClickListener, Cloudipsp.PayCallback, Cloudipsp.GooglePayCallback {

    private val RC_GOOGLE_PAY = 100500
    private val K_GOOGLE_PAY_CALL = "google_pay_call"
    private var cloudipsp: Cloudipsp? = null
    private var googlePayCall: GooglePayCall? = null // <- this should be serialized on saving instance state
    private lateinit var webView: CloudipspWebView
    private lateinit var googlePayButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Set the layout for this activity

        // Initialize UI elements
        webView = findViewById(R.id.webView) // Initialize CloudipspWebView from layout
        googlePayButton = findViewById(R.id.google_pay_button) // Initialize Button from layout
        googlePayButton.setOnClickListener(this) // Set click listener for Google Pay button

        // Check if Google Pay is supported and set button visibility accordingly
        if (Cloudipsp.supportsGooglePay(this)) {
            googlePayButton.visibility = View.VISIBLE // Show Google Pay button
        } else {
            googlePayButton.visibility = View.GONE // Hide Google Pay button if unsupported
            Toast.makeText(this, R.string.e_google_pay_unsupported, Toast.LENGTH_LONG).show() // Show unsupported message
        }
        if (savedInstanceState != null) {
            googlePayCall = savedInstanceState.getParcelable(K_GOOGLE_PAY_CALL)
        }
    }

    override fun onBackPressed() {
        if (webView.waitingForConfirm()) {
            webView.skipConfirm() // Skip confirmation in WebView if waiting
        } else {
            super.onBackPressed() // Otherwise, perform default back button behavior
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.google_pay_button) {
            processGooglePay() // Handle click on Google Pay button
        }
    }

    private fun processGooglePay() {
        // Initialize Cloudipsp with merchant ID and WebView
        cloudipsp = Cloudipsp(1396424, webView) // Initialize the payment process with the merchant ID
        val googlePayOrder = createOrder() // Create order for Google Pay payment
        googlePayOrder?.let {
            cloudipsp?.googlePayInitialize(it, this@MainActivityKotlin, RC_GOOGLE_PAY, this) // Initialize Google Pay payment
        }
    }

    private fun createOrder(): Order? {
        val amount = 100
        val email = "test@gmail.com"
        val description = "test payment"
        val currency = "GEL"
        return Order(amount, currency, "vb_${System.currentTimeMillis()}", description, email) // Create and return new payment order
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(K_GOOGLE_PAY_CALL, googlePayCall)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RC_GOOGLE_PAY -> {
                if (!cloudipsp?.googlePayComplete(resultCode, data, googlePayCall, this)!!) {
                    Toast.makeText(this, R.string.e_google_pay_canceled, Toast.LENGTH_LONG).show() // Show payment canceled message
                }
            }
        }
    }

    override fun onPaidProcessed(receipt: Receipt) {
        Toast.makeText(this, "Paid ${receipt.status.name}\nPaymentId:${receipt.paymentId}", Toast.LENGTH_LONG).show() // Show payment success message
    }

    override fun onPaidFailure(e: Cloudipsp.Exception) {
        when (e) {
            is Cloudipsp.Exception.Failure -> {
                Toast.makeText(this, "Failure\nErrorCode: ${e.errorCode}\nMessage: ${e.message}\nRequestId: ${e.requestId}", Toast.LENGTH_LONG).show() // Show specific failure details
            }
            is Cloudipsp.Exception.NetworkSecurity -> {
                Toast.makeText(this, "Network security error: ${e.message}", Toast.LENGTH_LONG).show() // Show network security error
            }
            is Cloudipsp.Exception.ServerInternalError -> {
                Toast.makeText(this, "Internal server error: ${e.message}", Toast.LENGTH_LONG).show() // Show internal server error
            }
            is Cloudipsp.Exception.NetworkAccess -> {
                Toast.makeText(this, "Network error", Toast.LENGTH_LONG).show() // Show network access error
            }
            else -> {
                Toast.makeText(this, "Payment Failed", Toast.LENGTH_LONG).show() // Show generic payment failure
            }
        }
        e.printStackTrace() // Print stack trace for debugging
    }

    override fun onGooglePayInitialized(result: GooglePayCall) {
        // Handle Google Pay initialization if needed
        Toast.makeText(this, "Google Pay initialized", Toast.LENGTH_LONG).show() // Show Google Pay initialization message
        googlePayCall = result // Store Google Pay call result
    }
}

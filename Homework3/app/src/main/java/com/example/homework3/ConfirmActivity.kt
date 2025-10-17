package com.example.homework3

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.homework3.databinding.ActivityConfirmBinding

class ConfirmActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConfirmBinding
    private lateinit var currentOrder: Order

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentOrder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("CURRENT_ORDER", Order::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("CURRENT_ORDER")!!
        }

        binding.btnConfirm.setOnClickListener {

            if (currentOrder.isComplete() != true) {
                Toast.makeText(
                    this,
                    "Please select a main meal, at least one side dish, and a drink.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("Submit Order")
                .setMessage(currentOrder.toSummary() + "\n\nSubmit this order?")
                .setPositiveButton("Submit") { d, _ ->
                    val resultIntent = Intent().apply {
                        putExtra("ORDER_RESULT", currentOrder)
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    d.dismiss()
                    Toast.makeText(this, "Order submitted!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
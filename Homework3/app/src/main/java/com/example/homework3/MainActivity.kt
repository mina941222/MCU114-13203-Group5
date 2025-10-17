package com.example.homework3

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.homework3.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentOrder: Order = Order()

    private val orderResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val updatedOrder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra("ORDER_RESULT", Order::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra("ORDER_RESULT")
            }
            updatedOrder?.let {
                currentOrder = it
                updateSummary()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateSummary()

        binding.btnMainMeal.setOnClickListener {
            startOrderActivity(MainMealActivity::class.java)
        }

        binding.btnSideDishes.setOnClickListener {
            startOrderActivity(SideDishesActivity::class.java)
        }

        binding.btnDrink.setOnClickListener {
            startOrderActivity(DrinkActivity::class.java)
        }

        binding.btnOrder.setOnClickListener {
            val intent = Intent(this, ConfirmActivity::class.java).apply {
                putExtra("CURRENT_ORDER", currentOrder)
            }
            orderResultLauncher.launch(intent)
        }
    }

    private fun startOrderActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass).apply {
            putExtra("CURRENT_ORDER", currentOrder)
        }
        orderResultLauncher.launch(intent)
    }

    private fun updateSummary() {
        val summaryText = currentOrder.toSummary()
        binding.tvCurrentSummary.text = "Current Selection:\n$summaryText"
    }

    override fun onResume() {
        super.onResume()
    }
}
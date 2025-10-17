package com.example.homework3

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.homework3.databinding.ActivityMainMealBinding

class MainMealActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainMealBinding
    private lateinit var currentOrder: Order

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainMealBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentOrder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("CURRENT_ORDER", Order::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("CURRENT_ORDER")!!
        }

        binding.btnNext.setOnClickListener {
            val selectedMainMeal = when (binding.radioGroupMainMeal.checkedRadioButtonId) {
                binding.rbBurger.id -> "Burger"
                binding.rbFriedChicken.id -> "Fried Chicken"
                binding.rbChickenNuggets.id -> "Chicken Nuggets"
                else -> null
            }
            currentOrder.mainMeal = selectedMainMeal

            val intent = Intent(this, SideDishesActivity::class.java).apply {
                putExtra("CURRENT_ORDER", currentOrder)
            }
            startActivityForResult(intent, REQUEST_CODE_ORDER_WIZARD)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ORDER_WIZARD && resultCode == RESULT_OK) {
            setResult(RESULT_OK, data)
            finish()
        }
    }

    companion object {
        private const val REQUEST_CODE_ORDER_WIZARD = 1
    }
}
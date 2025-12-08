package com.example.healthylife

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.healthylife.data.MealEntity

class MealAdapter(private var meals: List<MealEntity>) :
    RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_meal_name)
        val detailTextView: TextView = itemView.findViewById(R.id.tv_meal_detail)
        val caloriesTextView: TextView = itemView.findViewById(R.id.tv_calories)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_record, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = meals[position]

        // 🚨 修正：使用正確的屬性名稱 meal.type
        if (meal.type == "飲食") {
            // 飲食紀錄
            holder.nameTextView.text = "${meal.mealType}: ${meal.name}"
            holder.detailTextView.text = "@ ${meal.time} | 蛋白質 ${meal.protein} g"
            holder.caloriesTextView.text = "${meal.calories} kcal"
            // 熱量顏色
            holder.caloriesTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.color_calories))

        } else if (meal.type == "飲水") { // 🚨 修正：使用正確的屬性名稱 meal.type
            // 飲水紀錄
            holder.nameTextView.text = "💧 飲水紀錄"
            holder.detailTextView.text = "@ ${meal.time}"
            holder.caloriesTextView.text = "${meal.waterMl} ml" // 修正：使用 waterMl
            // 飲水顏色
            holder.caloriesTextView.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.color_water))
        } else {
            // 其它（例如 Feedback 的模擬數據）
            holder.nameTextView.text = meal.name
            holder.detailTextView.text = ""
            holder.caloriesTextView.text = ""
        }
    }

    override fun getItemCount(): Int = meals.size

    fun updateMeals(newMeals: List<MealEntity>) {
        meals = newMeals
        notifyDataSetChanged()
    }
}
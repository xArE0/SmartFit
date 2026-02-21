package com.example.project_smartfit.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Save / load / delete user-created custom workout plans.
 */
class CustomPlanRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("custom_plans", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun savePlan(plan: ExercisePlan) {
        val plans = getAllPlans().toMutableList()
        plans.removeAll { it.id == plan.id }
        plans.add(0, plan)
        prefs.edit().putString("plans", gson.toJson(plans)).apply()
    }

    fun getAllPlans(): List<ExercisePlan> {
        val json = prefs.getString("plans", null) ?: return emptyList()
        val type = object : TypeToken<List<ExercisePlan>>() {}.type
        return gson.fromJson(json, type)
    }

    fun deletePlan(planId: String) {
        val plans = getAllPlans().filter { it.id != planId }
        prefs.edit().putString("plans", gson.toJson(plans)).apply()
    }

    companion object {
        @Volatile private var instance: CustomPlanRepository? = null
        fun getInstance(context: Context): CustomPlanRepository =
            instance ?: synchronized(this) {
                instance ?: CustomPlanRepository(context.applicationContext).also { instance = it }
            }
    }
}

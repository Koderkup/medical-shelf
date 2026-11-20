package com.example.medical_shelf.data.local.entity

import androidx.annotation.StringRes
import com.example.medical_shelf.R

enum class BoxTemplate(
    @StringRes val titleResId: Int,
    val iconName: String,
    val isSystem: Boolean
) {
    HOME(R.string.box_template_home, "home", true),
    KIDS(R.string.box_template_kids, "child", true),
    TRAVEL(R.string.box_template_travel, "flight", true),
    CAR(R.string.box_template_car, "car", true),
    CUSTOM(R.string.box_add_new, "medical", false)
}
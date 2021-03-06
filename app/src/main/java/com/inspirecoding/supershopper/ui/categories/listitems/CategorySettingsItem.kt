package com.inspirecoding.supershopper.ui.categories.listitems

import android.view.View
import androidx.core.content.ContextCompat
import com.inspirecoding.supershopper.R
import com.inspirecoding.supershopper.data.Category
import com.inspirecoding.supershopper.databinding.LayoutCategorysettingsItemBinding
import com.inspirecoding.supershopper.utils.baseclasses.BaseItem
import com.inspirecoding.supershopper.utils.listOfCategoryIcons
import com.inspirecoding.supershopper.utils.listOfDefaultCategories

data class CategorySettingsItem(val category: Category) :
    BaseItem<LayoutCategorysettingsItemBinding> {

    override val layoutId = R.layout.layout_categorysettings_item
    override val uniqueId = category.id
    override val data = category

    override fun initializeViewBinding(view: View) = LayoutCategorysettingsItemBinding.bind(view)

    override fun bind(
        binding: LayoutCategorysettingsItemBinding,
        itemClickCallBack: ((View, BaseItem<LayoutCategorysettingsItemBinding>) -> Unit)?
    ) {
        val context = binding.root.context

        if (category.nameStringResId != null) {
            binding.tvCategory.text =
                context.getString(listOfDefaultCategories[category.nameStringResId].second)
        } else {
            binding.tvCategory.text = category.customName
        }

        binding.ivIcon.setImageDrawable(
            ContextCompat.getDrawable(
                context, listOfCategoryIcons[category.iconDrawableResId]
            )
        )

        binding.ivDelete.setOnClickListener {
            itemClickCallBack?.invoke(it, this)
        }

    }

}
package com.inspirecoding.supershopper.repository.local

import com.inspirecoding.supershopper.data.Category
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject


@ExperimentalCoroutinesApi
class ShopperRepositoryImpl @Inject constructor(
    private val shopperDao: ShopperDao
): ShopperRepository {

    override fun getCategories() = shopperDao.getCategories()

    override suspend fun insertCategory(category: Category) {
        shopperDao.insertCategory(category)
    }

    override suspend fun updateCategory(category: Category) {
        shopperDao.updateCategory(category)
    }

    override suspend fun updateCategories(categories: List<Category>) {
        shopperDao.updateCategories(categories)
    }

    override suspend fun deleteCategories(categories: List<Category>) {
        shopperDao.deleteCategories(categories)
    }

    override suspend fun deleteCategory(category: Category) {
        shopperDao.deleteCategory(category)
    }
}
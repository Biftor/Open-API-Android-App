package com.codingwithmitch.openapi.business.interactors.blog

import com.codingwithmitch.openapi.api.handleUseCaseException
import com.codingwithmitch.openapi.business.datasource.network.main.OpenApiMainService
import com.codingwithmitch.openapi.business.datasource.network.main.toBlogPost
import com.codingwithmitch.openapi.business.domain.models.AuthToken
import com.codingwithmitch.openapi.business.domain.models.BlogPost
import com.codingwithmitch.openapi.business.datasource.cache.blog.*
import com.codingwithmitch.openapi.presentation.main.blog.list.BlogFilterOptions
import com.codingwithmitch.openapi.presentation.main.blog.list.BlogOrderOptions
import com.codingwithmitch.openapi.business.domain.util.DataState
import com.codingwithmitch.openapi.business.domain.util.ErrorHandling.Companion.ERROR_AUTH_TOKEN_INVALID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class SearchBlogs(
    private val service: OpenApiMainService,
    private val cache: BlogPostDao,
) {

    private val TAG: String = "AppDebug"

    fun execute(
        authToken: AuthToken?,
        query: String,
        page: Int,
        filter: BlogFilterOptions,
        order: BlogOrderOptions,
    ): Flow<DataState<List<BlogPost>>> = flow {
        emit(DataState.loading<List<BlogPost>>())
        if(authToken == null){
            throw Exception(ERROR_AUTH_TOKEN_INVALID)
        }
        // get Blogs from network
        val filterAndOrder = order.value + filter.value // Ex: -date_updated
        val blogs = service.searchListBlogPosts(
            "Token ${authToken.token}",
            query = query,
            ordering = filterAndOrder,
            page = page
        ).results.map { it.toBlogPost() }

        // Insert into cache
        for(blog in blogs){
            try{
                cache.insert(blog.toEntity())
            }catch (e: Exception){
                e.printStackTrace()
            }
        }

        // emit from cache
        val cachedBlogs = cache.returnOrderedBlogQuery(
            query = query,
            filterAndOrder = filterAndOrder,
            page = page
        ).map { it.toBlogPost() }

        emit(DataState.data(response = null, data = cachedBlogs))
    }.catch { e ->
        emit(handleUseCaseException(e))
    }
}




















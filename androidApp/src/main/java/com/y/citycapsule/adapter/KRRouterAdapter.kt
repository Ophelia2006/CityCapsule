package com.y.citycapsule.adapter

import android.content.Context
import com.tencent.kuikly.core.render.android.adapter.IKRRouterAdapter
import com.y.citycapsule.navigation.AndroidRouteDispatcher
import org.json.JSONObject

object KRRouterAdapter : IKRRouterAdapter {

    override fun openPage(
        context: Context,
        pageName: String,
        pageData: JSONObject,
    ) {
        AndroidRouteDispatcher.shared.openPage(context, pageName, pageData)
    }

    override fun closePage(context: Context) {
        AndroidRouteDispatcher.shared.back(context)
    }
}

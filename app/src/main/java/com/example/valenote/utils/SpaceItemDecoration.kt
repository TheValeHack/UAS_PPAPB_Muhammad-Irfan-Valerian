package com.example.valenote.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        val layoutManager = parent.layoutManager

        if (layoutManager is GridLayoutManager) {
            val spanCount = layoutManager.spanCount
            val position = parent.getChildAdapterPosition(view)

            val column = position % spanCount

            outRect.left = space - column * space / spanCount
            outRect.right = (column + 1) * space / spanCount

            if (position < spanCount) {
                outRect.top = space
            }

            outRect.bottom = space
        } else {
            outRect.bottom = space
        }
    }
}

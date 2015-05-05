package info.guardianproject.zt.models;

import info.guardianproject.zt.widgets.PagedView;

import java.util.ArrayList;

import android.view.View;

public interface PagedViewContent
{
	boolean usesReverseSwipe();

	ArrayList<View> createPages(PagedView parent);
}

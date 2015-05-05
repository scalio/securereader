package info.guardianproject.zt.z.rss.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import info.guardianproject.zt.R;
import info.guardianproject.zt.z.rss.listeners.OnSwipeTouchListener;
import info.guardianproject.zt.z.rss.listeners.OnSwipeTouchListener2;
import info.guardianproject.zt.z.rss.utils.Feedback;
import info.guardianproject.zt.z.rss.utils.Font;

public class WriteTextFragment extends Fragment {

    private static final String TAG = "RadioZamaneh WriteTextFragment";
    public static boolean sDisableFragmentAnimations = false;

    private EditText writeText = null;
    public WriteTextFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View fragmentView = inflater.inflate(R.layout.zwrite_a_text_fragment, container, false);
        Typeface typeface = Font.getMitraBdFont(getActivity());
        writeText = (EditText) fragmentView.findViewById(R.id.writeText);
        writeText.setTypeface(typeface);
        ScrollView scrollView = (ScrollView) fragmentView.findViewById(R.id.scrollView);
        TextView writeTextTopLabelTextView = (TextView) fragmentView.findViewById(R.id.writeTextTopLabelTextView);
        writeTextTopLabelTextView.setTypeface(typeface);

        TextView writeTextLabel2TextView = (TextView) fragmentView.findViewById(R.id.writeTextLabel2TextView);
        writeTextLabel2TextView.setTypeface(typeface);

        Button saveButton =(Button) fragmentView.findViewById(R.id.saveButton);
        saveButton.setTypeface(typeface);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Feedback.sendEmail(getActivity(), getString(R.string.sendEmail) , "", writeText.getText().toString());
                Tab4ContainerFragment.self.restoreFromBackStack();
            }
        });

        OnSwipeTouchListener onSwipeTouchListener = new OnSwipeTouchListener(getActivity()){
            @Override
            public void onSwipeRight() {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(writeText.getWindowToken(), 0);
                Tab4ContainerFragment.self.restoreFromBackStack();
            }
        };
        OnSwipeTouchListener2 onSwipeTouchListener2 = new OnSwipeTouchListener2(getActivity()){
            @Override
            public void onSwipeRight() {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(writeText.getWindowToken(), 0);
                Tab4ContainerFragment.self.restoreFromBackStack();
            }
        };
        writeText.setOnTouchListener(onSwipeTouchListener2);
        scrollView.setOnTouchListener(onSwipeTouchListener);
        fragmentView.setOnTouchListener(onSwipeTouchListener);

        return fragmentView;
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(writeText.getWindowToken(), 0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        sDisableFragmentAnimations = true;
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        sDisableFragmentAnimations = false;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (sDisableFragmentAnimations) {
            Animation a = new Animation() {};
            a.setDuration(0);
            return a;
        }
        return super.onCreateAnimation(transit, enter, nextAnim);
    }
}

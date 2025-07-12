package com.example.mymusic.ui.favorites.bottomSheet;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mymusic.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class ArtistFilterBottomSheetFragment extends BottomSheetDialogFragment{
    private OnApplyListener applyListener;
    public interface OnApplyListener{
        void onApply();
    }
    public void setApplyListener(OnApplyListener applyListener){
        this.applyListener = applyListener;
    }
    private final String TAG = "ArtistFilterBottomSheetFragment";


    private TextView addedDateTextView, debutDateTextView, followersTextView, memberCountsTextView, artistNameTextView, imageCountsTextView;


    private TextView allTextView, over10YearsTextView,
            last5YearsTextView, last10YearsTextView, decade2020sTextView, decade2010sTextView, decade2000sTextView,
            before2000sTextView, seasonSpringTextView, seasonSummerTextView, seasonAutumnTextView, seasonWinterTextView,
            customInputTextView;

    private Map<String, TextView> sortTextViewMap;
    private Map<String, TextView> filterTextViewMap;


    private String sortOpt = null;
    private String filterOpt = null;

    private Button applyButton;
    private LinearLayout customInputLayout;
    private TextView dateStart, dateEnd;
    private SharedPreferences prefs;

    @Override
    public void onStart() {
        super.onStart();

        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED); // ← 강제 확장
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(null); // 기본 배경 제거
            }
        });

        return dialog;
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artist_filter_bottom_sheet, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@androidx.annotation.NonNull View view, @androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindView(view);
        setView();
        if (prefs == null){
            Context context = getContext();
            if (context != null) {
                prefs = context.getSharedPreferences("artist_filter_prefs", Context.MODE_PRIVATE);
            }
            String sortOption = prefs.getString("sort_option", "ADDED_DATE"); // 기본값 지정 가능
            String filterOption = prefs.getString("filter_option", "ALL");
            setupHighlight(sortOption, filterOption);

        }else{
            Log.d(TAG, "getContext() failed");
        }
        setupClickListeners();
        applyButton.setOnClickListener(v -> {
            setPrefs(sortOpt, filterOpt);
            dismiss();
            if (applyListener != null){
                applyListener.onApply();
            }
        });
    }

    private void bindView(View view){
        addedDateTextView = view.findViewById(R.id.added_date);
        debutDateTextView = view.findViewById(R.id.debut_date);
        followersTextView = view.findViewById(R.id.followers);
        memberCountsTextView = view.findViewById(R.id.member_counts);
        artistNameTextView = view.findViewById(R.id.artist_name);
        imageCountsTextView = view.findViewById(R.id.image_counts);

        allTextView = view.findViewById(R.id.all);
        over10YearsTextView = view.findViewById(R.id.over_10_year);
        last5YearsTextView = view.findViewById(R.id.last_5_years);
        last10YearsTextView = view.findViewById(R.id.last_10_years);
        decade2020sTextView = view.findViewById(R.id.decade_2020s);
        decade2010sTextView = view.findViewById(R.id.decade_2010s);
        decade2000sTextView = view.findViewById(R.id.decade_2000s);
        before2000sTextView = view.findViewById(R.id.before_2000s);
        seasonSpringTextView = view.findViewById(R.id.season_spring);
        seasonSummerTextView = view.findViewById(R.id.season_summer);
        seasonAutumnTextView = view.findViewById(R.id.season_autumn);
        seasonWinterTextView = view.findViewById(R.id.season_winter);
        customInputTextView = view.findViewById(R.id.custom_input);
        sortTextViewMap = new HashMap<>();
        sortTextViewMap.put("ADDED_DATE", addedDateTextView);
        sortTextViewMap.put("DEBUT_DATE", debutDateTextView);
        sortTextViewMap.put("FOLLOWERS", followersTextView);
        sortTextViewMap.put("MEMBER_COUNTS", memberCountsTextView);
        sortTextViewMap.put("ARTIST_NAME", artistNameTextView);
        sortTextViewMap.put("IMAGE_COUNTS", imageCountsTextView);

        filterTextViewMap = new HashMap<>();
        filterTextViewMap.put("ALL", allTextView);
        filterTextViewMap.put("LAST_5_YEARS", last5YearsTextView);
        filterTextViewMap.put("LAST_10_YEARS", last10YearsTextView);
        filterTextViewMap.put("OVER_10_YEARS", over10YearsTextView);
        filterTextViewMap.put("DECADE_2020S", decade2020sTextView);
        filterTextViewMap.put("DECADE_2010S", decade2010sTextView);
        filterTextViewMap.put("DECADE_2000S", decade2000sTextView);
        filterTextViewMap.put("BEFORE_2000S", before2000sTextView);
        filterTextViewMap.put("SEASON_SPRING", seasonSpringTextView);
        filterTextViewMap.put("SEASON_SUMMER", seasonSummerTextView);
        filterTextViewMap.put("SEASON_AUTUMN", seasonAutumnTextView);
        filterTextViewMap.put("SEASON_WINTER", seasonWinterTextView);
        filterTextViewMap.put("CUSTOM_INPUT", customInputTextView);

        applyButton = view.findViewById(R.id.apply_button);
        customInputLayout = view.findViewById(R.id.custom_input_layout);
        dateStart = view.findViewById(R.id.date_start);
        dateEnd = view.findViewById(R.id.date_end);
    }

    private void setView(){
        Context context = getContext();
        if (context != null) {
            if (prefs == null) {
                prefs = context.getSharedPreferences("artist_filter_prefs", Context.MODE_PRIVATE);
            }

            sortOpt = prefs.getString("sort_option", "ADDED_DATE");
            filterOpt = prefs.getString("filter_option", "ALL");
            setupHighlight(sortOpt, filterOpt);
            if (filterOpt.equals("CUSTOM_INPUT")){
                customInputLayout.setVisibility(View.VISIBLE);
            }

            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);


            String customStartDateStr = yesterday.toString();
            String customEndDateStr = today.toString();
            LocalDate customStartDate = LocalDate.parse(customStartDateStr);
            LocalDate customEndDate = LocalDate.parse(customEndDateStr);

            customStartDateStr = prefs.getString("start_date", yesterday.toString());

            customStartDate = LocalDate.parse(customStartDateStr);
            customEndDateStr = prefs.getString("end_date", today.toString());
            if (customEndDateStr.equals("TODAY")){
                customEndDate = LocalDate.now();
            } else{
                customEndDate = LocalDate.parse(customEndDateStr);

            }


            dateStart.setText(customStartDateStr);
            dateEnd.setText(customEndDateStr);

            LocalDate finalCustomStartDate = customStartDate;
            LocalDate finalCustomEndDate1 = customEndDate;
            dateStart.setOnClickListener(v -> {
                DatePickerDialog dialog = new DatePickerDialog(
                        context,
                        (view, year, month, dayOfMonth) -> {
                            // 사용자가 선택한 날짜 처리
                            // month는 0부터 시작하므로 +1 해줘야 함
                            LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                            if (selectedDate.isBefore(finalCustomEndDate1.plusDays(1))) {
                                dateStart.setText(selectedDate.toString());
                            }else{
                                dateStart.setText(finalCustomEndDate1.toString());
                                Toast.makeText(context, "종료 날짜는 시작 날짜보다 먼저일 수 없습니다", Toast.LENGTH_SHORT).show();
                            }
                        },
                        finalCustomStartDate.getYear(),
                        finalCustomStartDate.getMonthValue() - 1, // 여기서는 -1 필수 (DatePickerDialog는 0부터 시작)
                        finalCustomStartDate.getDayOfMonth()
                );
                dialog.show();

            });

            LocalDate finalCustomEndDate = customEndDate;
            LocalDate finalCustomStartDate1 = customStartDate;
            dateEnd.setOnClickListener(v -> {
                DatePickerDialog dialog = new DatePickerDialog(
                        context,
                        (view, year, month, dayOfMonth) -> {
                            // 사용자가 선택한 날짜 처리
                            // month는 0부터 시작하므로 +1 해줘야 함
                            LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                            if (selectedDate.isAfter(finalCustomStartDate1.minusDays(1))) {
                                dateEnd.setText(selectedDate.toString()); // 또는 원하는 포맷
                            }else{
                                dateEnd.setText(finalCustomStartDate1.toString()); // 또는 원하는 포맷
                                Toast.makeText(context, "종료 날짜는 시작 날짜보다 먼저일 수 없습니다", Toast.LENGTH_SHORT).show();
                            }
                            if (selectedDate.isAfter(today.minusDays(1))){
                                dateEnd.setText("TODAY"); // 또는 원하는 포맷
                            }
                        },
                        finalCustomEndDate.getYear(),
                        finalCustomEndDate.getMonthValue() - 1, // 여기서는 -1 필수 (DatePickerDialog는 0부터 시작)
                        finalCustomEndDate.getDayOfMonth()
                );
                dialog.show();

            });
        }
    }

    private void setupHighlight(String selectedSort, String selectedFilter) {
        if (selectedSort != null){
            // 모든 텍스트뷰 초기화
            for (TextView tv : sortTextViewMap.values()) {
                tv.setTypeface(null, Typeface.NORMAL);
                tv.setTextColor(Color.GRAY);
            }
            // 선택된 항목만 Bold 처리
            if (sortTextViewMap.containsKey(selectedSort)) {
                sortTextViewMap.get(selectedSort).setTypeface(null, Typeface.BOLD);
                sortTextViewMap.get(selectedSort).setTextColor(Color.DKGRAY);
            }
            sortOpt = selectedSort;
        }

        if (selectedFilter != null){
            for (TextView tv : filterTextViewMap.values()) {
                tv.setTypeface(null, Typeface.NORMAL);
                tv.setTextColor(Color.GRAY);
            }
            if (filterTextViewMap.containsKey(selectedFilter)) {
                filterTextViewMap.get(selectedFilter).setTypeface(null, Typeface.BOLD);
                filterTextViewMap.get(selectedFilter).setTextColor(Color.DKGRAY);
            }
            filterOpt = selectedFilter;
        }

    }

    private void setupClickListeners() {
        for (Map.Entry<String, TextView> entry : sortTextViewMap.entrySet()) {
            String key = entry.getKey();
            TextView tv = entry.getValue();
            tv.setOnClickListener(v -> {
                setupHighlight(key, null); // filterOpt 유지
                setPrefs(key, null);
            });
        }

        for (Map.Entry<String, TextView> entry : filterTextViewMap.entrySet()) {
            String key = entry.getKey();
            TextView tv = entry.getValue();
            tv.setOnClickListener(v -> {
                if (key.equals("CUSTOM_INPUT")){
                    customInputLayout.setVisibility(View.VISIBLE);
                }
                else{
                    customInputLayout.setVisibility(View.GONE);
                }
                setupHighlight(null, key); // sortOpt 유지
                setPrefs(null, key);
            });

        }
    }

    private void setPrefs(String sortOption, String filterOption){
        Context context = getContext();
        if (context == null) {
            Log.d(TAG, "getContext() failed");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences("artist_filter_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("sort_option", sortOption);
        editor.putString("filter_option", filterOption);
        editor.putString("start_date", dateStart.getText().toString());
        if (dateEnd.getText().toString().equals(LocalDate.now().toString())){
            editor.putString("end_date", "TODAY");
        } else {
            editor.putString("end_date", dateEnd.getText().toString());
        }
        editor.apply(); // 또는 commit()
    }
}

package com.example.code_zombom_app;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Full-screen activity that lets entrants choose filter options (interests and availability).
 * Returns the selected {@link FilterSortState} to the caller via activity result.
 */
public class FilterSortActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_STATE = "filter_sort_initial_state";
    public static final String EXTRA_RESULT_STATE = "filter_sort_result_state";
    public static final String EXTRA_RESULT_RESET = "filter_sort_result_reset";

    private static final String STATE_WORKING = "filter_sort_working_state";

    private FilterSortState workingState;

    private CheckBox checkBoxInterests;
    private LinearLayout containerInterests;
    private RadioGroup radioGroupInterests;
    private CheckBox checkBoxAvailability;
    private LinearLayout containerAvailability;
    private TextView textSelectedRange;

    private final SimpleDateFormat dateFormatter =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_filter_sort_activity);

        restoreState(savedInstanceState);
        bindViews();
        initializeUi();
        attachListeners();
    }

    /**
     * Restores the working state from a rotation or from the caller's extras.
     */
    private void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            workingState = FilterSortState.copyOf(
                    (FilterSortState) savedInstanceState.getSerializable(STATE_WORKING));
        } else {
            workingState = FilterSortState.copyOf(
                    (FilterSortState) getIntent().getSerializableExtra(EXTRA_INITIAL_STATE));
        }
        if (workingState == null) {
            workingState = new FilterSortState();
        }
    }

    /**
     * Locates child views and attaches the primary button listeners.
     */
    private void bindViews() {
        checkBoxInterests = findViewById(R.id.checkboxFilterInterests);
        containerInterests = findViewById(R.id.containerInterests);
        radioGroupInterests = findViewById(R.id.radioGroupInterests);
        checkBoxAvailability = findViewById(R.id.checkboxFilterAvailability);
        containerAvailability = findViewById(R.id.containerAvailability);
        textSelectedRange = findViewById(R.id.textSelectedRange);

        Button buttonSelectRange = findViewById(R.id.buttonSelectRange);
        Button buttonApply = findViewById(R.id.buttonApply);
        Button buttonReset = findViewById(R.id.buttonReset);

        buttonSelectRange.setOnClickListener(v -> showDateRangePicker());
        buttonApply.setOnClickListener(v -> returnResult(false));
        buttonReset.setOnClickListener(v -> returnResetResult());
    }

    /**
     * Applies the current working state to the visible controls.
     */
    private void initializeUi() {
        // Interest filters.
        checkBoxInterests.setChecked(workingState.isFilterByInterests());
        containerInterests.setVisibility(workingState.isFilterByInterests() ? LinearLayout.VISIBLE : LinearLayout.GONE);
        updateInterestSelection();

        // Availability filters.
        checkBoxAvailability.setChecked(workingState.isFilterByAvailability());
        containerAvailability.setVisibility(workingState.isFilterByAvailability() ? LinearLayout.VISIBLE : LinearLayout.GONE);
        updateAvailabilitySummary();
    }

    /**
     * Configures listeners for toggles and radio groups driving the working state.
     */
    private void attachListeners() {
        checkBoxInterests.setOnCheckedChangeListener((buttonView, isChecked) -> {
            workingState.setFilterByInterests(isChecked);
            containerInterests.setVisibility(isChecked ? LinearLayout.VISIBLE : LinearLayout.GONE);
            if (!isChecked) {
                radioGroupInterests.clearCheck();
            }
        });

        radioGroupInterests.setOnCheckedChangeListener((group, checkedId) -> {
            String category = mapInterestRadioToCategory(checkedId);
            workingState.setSelectedInterestCategory(category);
        });

        checkBoxAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            workingState.setFilterByAvailability(isChecked);
            containerAvailability.setVisibility(isChecked ? LinearLayout.VISIBLE : LinearLayout.GONE);
            if (!isChecked) {
                workingState.setAvailabilityStart(null);
                workingState.setAvailabilityEnd(null);
            }
            updateAvailabilitySummary();
        });
    }

    /**
     * Opens the start-date picker and chains to the end-date picker when a start is chosen.
     */
    private void showDateRangePicker() {
        final Calendar startCalendar = Calendar.getInstance();
        Date existingStart = workingState.getAvailabilityStart();
        if (existingStart != null) {
            startCalendar.setTime(existingStart);
        }

        DatePickerDialog startPicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar chosenStart = Calendar.getInstance();
                    chosenStart.set(year, month, dayOfMonth, 0, 0, 0);
                    chosenStart.set(Calendar.MILLISECOND, 0);
                    workingState.setAvailabilityStart(chosenStart.getTime());
                    showEndDatePicker(chosenStart.getTime());
                },
                startCalendar.get(Calendar.YEAR),
                startCalendar.get(Calendar.MONTH),
                startCalendar.get(Calendar.DAY_OF_MONTH)
        );
        startPicker.getDatePicker().setMinDate(System.currentTimeMillis());
        startPicker.show();
    }

    /**
     * Opens the end-date picker constrained to dates on/after the supplied start date.
     */
    private void showEndDatePicker(Date startDate) {
        final Calendar endCalendar = Calendar.getInstance();
        Date existingEnd = workingState.getAvailabilityEnd();
        if (existingEnd != null && !existingEnd.before(startDate)) {
            endCalendar.setTime(existingEnd);
        } else {
            endCalendar.setTime(startDate);
        }

        DatePickerDialog endPicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar chosenEnd = Calendar.getInstance();
                    chosenEnd.set(year, month, dayOfMonth, 23, 59, 59);
                    chosenEnd.set(Calendar.MILLISECOND, 999);
                    Date endDate = chosenEnd.getTime();
                    if (endDate.before(startDate)) {
                        Toast.makeText(this, R.string.filter_sort_range_error, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    workingState.setFilterByAvailability(true);
                    workingState.setAvailabilityStart(startDate);
                    workingState.setAvailabilityEnd(endDate);
                    checkBoxAvailability.setChecked(true);
                    updateAvailabilitySummary();
                },
                endCalendar.get(Calendar.YEAR),
                endCalendar.get(Calendar.MONTH),
                endCalendar.get(Calendar.DAY_OF_MONTH)
        );
        endPicker.getDatePicker().setMinDate(startDate.getTime());
        endPicker.show();
    }

    /**
     * Syncs the interest radio group to the current working state.
     */
    private void updateInterestSelection() {
        String category = workingState.getSelectedInterestCategory();
        if (TextUtils.isEmpty(category)) {
            radioGroupInterests.clearCheck();
            return;
        }
        int radioId = mapCategoryToRadio(category);
        if (radioId != RadioGroup.NO_ID) {
            radioGroupInterests.check(radioId);
        } else {
            radioGroupInterests.clearCheck();
        }
    }

    /**
     * Updates the availability summary text to reflect the selected range.
     */
    private void updateAvailabilitySummary() {
        Date start = workingState.getAvailabilityStart();
        Date end = workingState.getAvailabilityEnd();

        if (!workingState.isFilterByAvailability() || start == null) {
            textSelectedRange.setText(R.string.filter_sort_range_placeholder);
            return;
        }

        if (end == null) {
            textSelectedRange.setText(dateFormatter.format(start));
        } else {
            String summary = String.format(
                    Locale.getDefault(),
                    "%s - %s",
                    dateFormatter.format(start),
                    dateFormatter.format(end)
            );
            textSelectedRange.setText(summary);
        }
    }

    @Nullable
    private String mapInterestRadioToCategory(int radioId) {
        if (radioId == R.id.radioInterestSport) {
            return "Sport";
        } else if (radioId == R.id.radioInterestEsport) {
            return "eSport";
        } else if (radioId == R.id.radioInterestFood) {
            return "Food";
        } else if (radioId == R.id.radioInterestMusic) {
            return "Music";
        } else if (radioId == R.id.radioInterestEngineering) {
            return "Engineering";
        }
        return null;
    }

    /**
     * Maps a normalized category label back to its radio button id.
     */
    private int mapCategoryToRadio(@Nullable String category) {
        if (category == null) {
            return RadioGroup.NO_ID;
        }
        switch (category) {
            case "Sport":
                return R.id.radioInterestSport;
            case "eSport":
                return R.id.radioInterestEsport;
            case "Food":
                return R.id.radioInterestFood;
            case "Music":
                return R.id.radioInterestMusic;
            case "Engineering":
                return R.id.radioInterestEngineering;
            default:
                return RadioGroup.NO_ID;
        }
    }

    /**
     * Clears the working state and returns a reset result to the caller.
     */
    private void returnResetResult() {
        workingState = new FilterSortState();
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_STATE, workingState);
        result.putExtra(EXTRA_RESULT_RESET, true);
        setResult(RESULT_OK, result);
        finish();
    }

    /**
     * Returns the current working state to the caller.
     *
     * @param resetFlag whether the state was produced by an explicit reset action
     */
    private void returnResult(boolean resetFlag) {
        Intent result = new Intent();
        result.putExtra(EXTRA_RESULT_STATE, FilterSortState.copyOf(workingState));
        result.putExtra(EXTRA_RESULT_RESET, resetFlag);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_WORKING, FilterSortState.copyOf(workingState));
    }
}

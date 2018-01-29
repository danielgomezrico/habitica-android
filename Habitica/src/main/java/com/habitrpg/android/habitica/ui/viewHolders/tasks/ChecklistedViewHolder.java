package com.habitrpg.android.habitica.ui.viewHolders.tasks;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.habitrpg.android.habitica.R;
import com.habitrpg.android.habitica.events.commands.ChecklistCheckedCommand;
import com.habitrpg.android.habitica.events.commands.TaskCheckedCommand;
import com.habitrpg.android.habitica.helpers.RxErrorHandler;
import com.habitrpg.android.habitica.models.tasks.ChecklistItem;
import com.habitrpg.android.habitica.models.tasks.Task;
import com.habitrpg.android.habitica.ui.helpers.MarkdownParser;

import net.pherth.android.emoji_library.EmojiTextView;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public abstract class ChecklistedViewHolder extends BaseTaskViewHolder implements CompoundButton.OnCheckedChangeListener {

    private static Integer expandedChecklistRow = null;

    @BindView(R.id.checkBoxHolder)
    ViewGroup checkboxHolder;
    @BindView(R.id.checkBox)
    CheckBox checkbox;
    @BindView(R.id.checklistView)
    LinearLayout checklistView;
    @BindView(R.id.checklistBottomSpace)
    View checklistBottomSpace;
    @BindView(R.id.checklistIndicatorWrapper)
    ViewGroup checklistIndicatorWrapper;
    @BindView(R.id.checkListCompletedTextView)
    TextView checklistCompletedTextView;
    @BindView(R.id.checkListAllTextView)
    TextView checklistAllTextView;

    public ChecklistedViewHolder(View itemView) {
        super(itemView);
        checklistIndicatorWrapper.setClickable(true);
        checkbox.setOnCheckedChangeListener(this);
        expandCheckboxTouchArea(checkboxHolder, checkbox);
    }

    @Override
    public void bindHolder(Task newTask, int position) {
        boolean completed = newTask.getCompleted();
        if (newTask.isPendingApproval()) {
            completed = false;
        }
        this.checkbox.setChecked(completed);
        if (this.shouldDisplayAsActive(newTask) && !newTask.isPendingApproval()) {
            this.checkboxHolder.setBackgroundResource(newTask.getLightTaskColor());
        } else {
            this.checkboxHolder.setBackgroundColor(this.taskGray);
        }
        this.checklistCompletedTextView.setText(String.valueOf(newTask.getCompletedChecklistCount()));
        this.checklistAllTextView.setText(String.valueOf(newTask.getChecklist().size()));

        this.checklistView.removeAllViews();
        this.updateChecklistDisplay();

        this.checklistIndicatorWrapper.setVisibility(newTask.getChecklist().size() == 0 ? View.GONE : View.VISIBLE);
        if (this.rightBorderView != null) {
            this.rightBorderView.setVisibility(newTask.getChecklist().size() == 0 ? View.VISIBLE : View.GONE);
            if (newTask.getCompleted()) {
                this.rightBorderView.setBackgroundResource(newTask.getLightTaskColor());
            } else {
                this.rightBorderView.setBackgroundColor(this.taskGray);
            }
        }
        super.bindHolder(newTask, position);
    }

    abstract public Boolean shouldDisplayAsActive(Task newTask);

    public void updateChecklistDisplay() {
        //This needs to be a LinearLayout, as ListViews can not be inside other ListViews.
        if (this.checklistView != null) {
            if (this.shouldDisplayExpandedChecklist()) {
                LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if (this.task.getChecklist().isValid()) {
                    for (ChecklistItem item : this.task.getChecklist()) {
                        LinearLayout itemView = (LinearLayout) layoutInflater.inflate(R.layout.checklist_item_row, this.checklistView, false);
                        CheckBox checkbox = itemView.findViewById(R.id.checkBox);
                        EmojiTextView textView = itemView.findViewById(R.id.checkedTextView);
                        // Populate the data into the template view using the data object
                        textView.setText(item.getText());

                        Observable.just(item.getText())
                                .map(MarkdownParser::parseMarkdown)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(textView::setText, RxErrorHandler.handleEmptyError());
                        checkbox.setChecked(item.getCompleted());
                        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            ChecklistCheckedCommand event = new ChecklistCheckedCommand();
                            event.task = task;
                            event.item = item;
                            EventBus.getDefault().post(event);
                        });
                        ViewGroup checkboxHolder = (ViewGroup) itemView.findViewById(R.id.checkBoxHolder);
                        expandCheckboxTouchArea(checkboxHolder, checkbox);
                        this.checklistView.addView(itemView);
                    }
                }
                this.checklistView.setVisibility(View.VISIBLE);
                this.checklistBottomSpace.setVisibility(View.VISIBLE);
            } else {
                this.checklistView.removeAllViewsInLayout();
                this.checklistView.setVisibility(View.GONE);
                this.checklistBottomSpace.setVisibility(View.GONE);
            }
        }
    }

    @OnClick(R.id.checklistIndicatorWrapper)
    public void onChecklistIndicatorClicked() {
        expandedChecklistRow = this.shouldDisplayExpandedChecklist() ? null : getAdapterPosition();
        if (this.shouldDisplayExpandedChecklist()) {
            RecyclerView recyclerView = (RecyclerView) this.checklistView.getParent().getParent();
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            layoutManager.scrollToPositionWithOffset(this.getAdapterPosition(), 15);
        }
        updateChecklistDisplay();

    }

    private boolean shouldDisplayExpandedChecklist() {
        return expandedChecklistRow != null && getAdapterPosition() == expandedChecklistRow;
    }

    public void expandCheckboxTouchArea(final View expandedView, final View checkboxView) {
        expandedView.post(() -> {
            Rect rect = new Rect();
            expandedView.getHitRect(rect);
            expandedView.setTouchDelegate(new TouchDelegate(rect, checkboxView));
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.equals(checkbox)) {
            if (!task.isValid()) {
                return;
            }
            if (isChecked != task.getCompleted()) {
                TaskCheckedCommand event = new TaskCheckedCommand();
                event.Task = task;
                event.completed = !task.getCompleted();

                // it needs to be changed after the event is send -> to the server
                // maybe a refactor is needed here
                EventBus.getDefault().post(event);
            }
        }
    }

    @Override
    public void setDisabled(boolean openTaskDisabled, boolean taskActionsDisabled) {
        super.setDisabled(openTaskDisabled, taskActionsDisabled);

        this.checkbox.setEnabled(!taskActionsDisabled);
    }
}

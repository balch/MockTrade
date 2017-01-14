PresenterActivity in Android MVP
===============================
There are almost as many Android MVP patterns as there are Android applications. Most Android MVP Architectures implement the View
logic in the Activity. This doc makes the case for putting the view logic in custom view classes and turing the Activity into
the Presenter.

### Other Android MVP Approaches

#### [Mosby MVP](http://hannesdorfmann.com/mosby/mvp/)
Mosby is an interesting Android MVP Framework that has a lot of cool functionality. I like that version
2.0 is leaner and meaner, and the ViewState and Loading-Content-Error features show they understand
the complexities of developing Android applications.

I found this quote which nicely describes their MVP philosophy:
> But before we dive deeper in how to implement MVP on Android we have to clarify if an Activity or
Fragment is a View or a Presenter. Activity and Fragment seems to be both, because they have
lifecycle callbacks like onCreate() or onDestroy() as well as responsibilities of View things
like switching from one UI widget to another UI widget (like showing a ProgressBar while loading
and then displaying a ListView with data). You may say that these sounds like an Activity or
Fragment is a Controller. However, we came to the conclusion that Activity and Fragment
should be treated as part of a (dumb) View and not as a Presenter. You will see why afterwards.

This quote asks the right question, but comes up with a different answer. They are correct
that the Activity/Fragment provide both Presenter and View methods and behavior, but, I would
argue that the View methods (mainly `.findViewById()`) are convenience methods that are most
useful for creating Android sample applications.

The `Activity.setContentView(View view)` is the primary example I use to make the case
that the view is conceptually separate from the Activity. The View is passed to the Activity and
held as a reference (by adding it to the Window object). Calls to `Activity.findViewById()`
are delegated to the stored View (since both View and Activity implement `.findViewById()`).

Storing another reference to the View in a separate Presenter class adds another level
of indirection: Activity (Lifecycle Events) --&gt; Presenter --&gt; View
(which is actually the Activity). I first tried this approach in an application using
complex Lifecycle Events tied to Loaders and `onActivityResult()`. I found the
Presenter eventually morphed into a subset of Activity Lifecycle Events codified into an
interface.

This same concept also applies to Fragments and is reinforced by the `Fragment.onCreateView()`
method which forces the Fragment derived class to create a separate view.


#### [GoogleSamples todo-mvp](https://github.com/googlesamples/android-architecture/tree/todo-mvp/)

* [View Logic implemented in Activity/Fragment](https://github.com/googlesamples/android-architecture/blob/todo-mvp/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/tasks/TasksFragment.java)
* [Presenter-View Contract](https://github.com/googlesamples/android-architecture/blob/todo-mvp/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/tasks/TasksContract.java)
```java
// This specifies the contract between the view and the presenter.
public interface TasksContract {
    interface View extends BaseView<Presenter> {
        void setLoadingIndicator(boolean active);
        void showTasks(List<Task> tasks);
        void showAddTask();
        ...
    }

    interface Presenter extends BasePresenter {
        void result(int requestCode, int resultCode);
        void loadTasks(boolean forceUpdate);
        void addNewTask();
        ...
    }
}
```

*[Presenter-View cross reference](https://github.com/googlesamples/android-architecture/blob/todo-mvp/todoapp/app/src/main/java/com/example/android/architecture/blueprints/todoapp/tasks/TasksPresenter.java)
```java
public class TasksPresenter implements TasksContract.Presenter {

    private final TasksContract.View mTasksView;
    public TasksPresenter(@NonNull TasksRepository tasksRepository, @NonNull TasksContract.View tasksView) {
        mTasksView.setPresenter(this);
    }
}
```

### PresenterActivity
* Activity Lifecycle is the heart of Android Application
   * Presenter state persistence
* Forces MVP with `abstract V createView()`
* Error Handling
* Android Lifecycle Logging and Timing
```java
public abstract class PresenterActivity<V extends View & BaseView, M extends ModelProvider>
                            extends AppCompatActivity  {

    protected abstract V createView();
    protected abstract void createModel(M modelProvider);

    // override-able activity functions
    public void onCreateBase(Bundle savedInstanceState) {}
    public void onResumeBase() {}
    public void onPauseBase() {}
    public void onStartBase() {}
    public void onStopBase() {}
    public void onDestroyBase() {}
    public void onSaveInstanceStateBase(Bundle outState) {}
    public void onActivityResultBase(int requestCode, int resultCode, Intent data) { }
    public boolean onHandleException(String logMsg, Exception ex) {return false;}
    ...
    ...
    ...
}
```

### Model and ModelProvider
* Domain Objects
* ModelProvider
   * All Application Level Objects
       * Sql Connection
       * Http Connection
   * Facilitate Dependency Injection
* Model Interactors

### View
* Listener interface
   * acts as a mini-presenter
   * allows activity to call through models and set data in view
* Reusable to display data from other sources
* Con: damn &lt;merge&gt; tags
```java
public class AuctionView extends LinearLayout implements BaseView {
    ...
    ...
    public interface MainViewListener {
        boolean onLoadMore(int currentPage);
        void onChangeSort(int position);
        void onClickNoteButton(Auction auction);
        void onClickAuction(Auction auction);
        void onClickSearch(String keyword);
    }

    protected MainViewListener mainViewListener;

    public AuctionView(Context context) {
        super(context);
    }

    public AuctionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AuctionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void initializeLayout() {
         inflate(getContext(), R.layout.auction_view, this);
         ...
    }

    public void setMainViewListener(MainViewListener mainViewListener) {
        this.mainViewListener = mainViewListener;
    }
    ...
    ...
}
```

```java
public class MainActivity extends PresenterActivity<AuctionView, AuctionModelProvider> {

    protected AuctionView auctionView;
    protected EBayModel auctionModel;
    protected NotesModel notesModel;

    @Override
    public void onCreateBase(Bundle bundle) {
        this.auctionView.setMainViewListener(this);
        this.auctionView.showBusy();
    }

    @Override
    public AuctionView createView() {
        auctionView = new AuctionView(this);
        return auctionView;
    }

    @Override
    protected void createModel(AuctionModelProvider modelProvider) {
        auctionModel = new EBayModel(getString(R.string.ebay_app_id), modelProvider);
        notesModel = new NotesModel(modelProvider);
    }

    ...
    ...
}
```

### Unit Testing
* Lifecycle *Base methods do not call super()
    * can be used w/o robo
* Dependency Injection with ModelProvider
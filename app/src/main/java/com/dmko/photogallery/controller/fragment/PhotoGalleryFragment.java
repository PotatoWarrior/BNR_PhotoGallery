package com.dmko.photogallery.controller.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.dmko.photogallery.R;
import com.dmko.photogallery.controller.activity.PhotoPageActivity;
import com.dmko.photogallery.controller.notification.AlarmPollService;
import com.dmko.photogallery.controller.notification.JobPollService;
import com.dmko.photogallery.model.FlickrFetcher;
import com.dmko.photogallery.model.GalleryItem;
import com.dmko.photogallery.model.QueryPreferences;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {
    private static final String TAG = "PhotoGalleryFragment";
    private static final int COLUMN_WIDTH = 300;

    private static int mPage = 1;

    private RecyclerView mPhotoRecyclerView;
    private PhotoAdapter mAdapter;
    private List<GalleryItem> mItems = new ArrayList<>();


    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) view.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mPhotoRecyclerView.getLayoutManager() == null) {
                    int width = mPhotoRecyclerView.getWidth();
                    mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), width / COLUMN_WIDTH));
                    setupAdapter();
                }
            }
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.photo_gallery_search, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: " + query);

                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                searchView.onActionViewCollapsed();

                QueryPreferences.setStoredQuery(getActivity(), query);
                mPage = 1;
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "onQueryTextChange: " + newText);
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);

        boolean isActive;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            isActive = AlarmPollService.isServiceAlarmOn(getActivity());
        } else {
            isActive = JobPollService.isJobActive(getActivity());
            Log.i(TAG, "onCreateOptionsMenu: " + isActive);
        }

        if (isActive) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                mPage = 1;
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                    boolean shouldStartAlarm = !AlarmPollService.isServiceAlarmOn(getActivity());
                    AlarmPollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                } else {
                    boolean shouldStartJob = !JobPollService.isJobActive(getActivity());
                    JobPollService.setJob(getActivity(), shouldStartJob);
                }
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemTask(query).execute();
    }

    private void setupAdapter() {
        if (isAdded() && !mItems.isEmpty()) {
            mAdapter = new PhotoAdapter(mItems);
            mPhotoRecyclerView.setAdapter(mAdapter);
            mAdapter.setOnLoadMoreListener(new OnLoadMoreListener() {
                @Override
                public void onLoadMore() {
                    mItems.add(null);
                    mPhotoRecyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.notifyItemInserted(mItems.size() - 1);
                        }
                    });
                    updateItems();
                }
            });
        }
    }

    private void updateAdapter() {
        mAdapter.notifyDataSetChanged();
        mAdapter.setLoaded();
    }

    private class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        private String mQuery;

        public FetchItemTask(String query) {
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            if (mQuery == null) {
                Log.i(TAG, "doInBackground: Fetching recent photos, page " + mPage);
                return new FlickrFetcher().fetchRecentPhotos(mPage++);
            } else {
                Log.i(TAG, "doInBackground: Searching for photos, query = " + mQuery + " page = " + mPage);
                return new FlickrFetcher().searchPhotos(mQuery, mPage++);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            if (mPage == 2) {
                mItems = items;
                setupAdapter();
            } else {
                mItems.remove(mItems.size() - 1);
                mItems.addAll(items);
                updateAdapter();
            }
        }
    }


    private class PhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final int VIEW_ITEM = 1;
        private final int VIEW_PROG = 2;

        private List<GalleryItem> items;
        private int visibleThreshold = 6;
        private int lastVisibleItem, totalItemCount;
        private boolean loading;
        private OnLoadMoreListener mOnLoadMoreListener;

        public PhotoAdapter(List<GalleryItem> items) {
            this.items = items;
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();

            mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    totalItemCount = gridLayoutManager.getItemCount();
                    lastVisibleItem = gridLayoutManager.findLastVisibleItemPosition();

                    if (!loading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                        if (mOnLoadMoreListener != null) {
                            System.out.println("listener");
                            mOnLoadMoreListener.onLoadMore();
                        }
                        loading = true;
                    }
                }
            });
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position) != null ? VIEW_ITEM : VIEW_PROG;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView.ViewHolder viewHolder;
            if (viewType == VIEW_ITEM) {
                View v = LayoutInflater.from(getActivity()).inflate(R.layout.list_item_gallery, parent, false);
                viewHolder = new PhotoHolder(v);
            } else {
                View v = LayoutInflater.from(getActivity()).inflate(R.layout.list_item_loading_in_progress, parent, false);
                viewHolder = new ProgressHolder(v);
            }
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof PhotoHolder) {
                GalleryItem galleryItem = items.get(position);
                PhotoHolder photoHolder = (PhotoHolder) holder;

                Picasso.with(getActivity())
                        .load(galleryItem.getUrl())
                        .placeholder(R.drawable.bill_up_close)
                        .into(photoHolder.mItemImageView);

                photoHolder.bindGalleryItem(galleryItem);
            } else if (holder instanceof ProgressHolder) {
                ((ProgressHolder) holder).mProgressBar.setIndeterminate(true);
            }
        }

        public void setLoaded() {
            loading = false;
        }

        public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
            mOnLoadMoreListener = onLoadMoreListener;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
            private ImageView mItemImageView;
            private GalleryItem mGalleryItem;

            public PhotoHolder(View itemView) {
                super(itemView);
                mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
                mItemImageView.setOnClickListener(this);
            }

            public void bindGalleryItem(GalleryItem galleryItem) {
                mGalleryItem = galleryItem;
            }

            @Override
            public void onClick(View v) {
                startActivity(PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri()));
            }
        }

        public class ProgressHolder extends RecyclerView.ViewHolder {
            public ProgressBar mProgressBar;

            public ProgressHolder(View itemView) {
                super(itemView);
                mProgressBar = (ProgressBar) itemView.findViewById(R.id.progress_bar);
            }
        }
    }
}

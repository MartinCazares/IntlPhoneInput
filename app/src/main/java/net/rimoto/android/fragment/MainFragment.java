package net.rimoto.android.fragment;

import android.content.Intent;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.rimoto.android.R;
import net.rimoto.android.activity.WizardActivity_;
import net.rimoto.android.adapter.TagsRecycleAdapter;
import net.rimoto.core.API;
import net.rimoto.core.models.Policy;
import net.rimoto.vpnlib.VpnManager;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.parceler.Parcels;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

@EFragment(R.layout.fragment_main)
public class MainFragment extends Fragment {
    public static final String EXTRA_POLICIES = "EXTRA_POLICIES";

    @ViewById(R.id.tagsRecycler)
    protected RecyclerView mTagsRecycler;

    @ViewById(R.id.actionBtn)
    protected Button mActionButton;

    @ViewById(R.id.main_boxa_text)
    protected TextView mMainBoxaText;

    @AfterViews
    protected void afterViews() {
        mTagsRecycler.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this.getActivity());
        mTagsRecycler.setLayoutManager(linearLayoutManager);

        if(!VpnManager.isActive(getActivity())) {
            setAsPreview();
        }

        initiatePolicies();
    }

    /**
     * Fet policies from Intent if available,
     *  Otherwise fetch them
     */
    private void initiatePolicies() {
        Intent intent = getActivity().getIntent();
        if(intent.getExtras() != null) {
            Parcelable policiesParcel = (Parcelable) intent.getExtras().get(EXTRA_POLICIES);
            if(policiesParcel != null) {
                List<Policy> policies = Parcels.unwrap(policiesParcel);
                setPolicies(policies);
            } else {
                fetchPolicies();
            }
        } else {
            fetchPolicies();
        }
    }

    /**
     * Fetching policies from API
     */
    private void fetchPolicies() {
        API.getInstance().getPolicies(new Callback<List<Policy>>() {
            public void success(List<Policy> policies, Response response) {
                setPolicies(policies);
            }
            public void failure(RetrofitError error) {
                error.printStackTrace();
            }
        });
    }

    /**
     * Set the policies
     * @param policies List<Policy>
     */
    public void setPolicies(List<Policy> policies) {
        if(mTagsRecycler !=null) {
            TagsRecycleAdapter adapter = new TagsRecycleAdapter(policies, true);
            mTagsRecycler.setAdapter(adapter);
        }

        // Show wizard btn if not connected!
        if(mPageState != PageState.Preview) {
            //Set as premium if non "appPolicy" exists
            for (Policy policy : policies) {
                if (!policy.getName().equals(TagsRecycleAdapter.FREE_POLICY_NAME)) {
                    setAsPremium();
                    break;
                }
            }
        }

        //Show BTN after data initiated
        if (mActionButton != null) {
            mActionButton.setVisibility(View.VISIBLE);
        }
    }

    private enum PageState {Preview, ConnectedFree, ConnectedPremium }
    private PageState mPageState = PageState.ConnectedFree;

    private void setAsPreview() {
        mPageState = PageState.Preview;
        if(mActionButton !=null) {
            mActionButton.setText(R.string.actionBtn_preview);
        }
        if(mMainBoxaText != null) {
            mMainBoxaText.setText(R.string.main_boxa_text_preview);
        }
    }
    private void setAsPremium() {
        mPageState = PageState.ConnectedPremium;
        if(mActionButton !=null) {
            mActionButton.setText(R.string.actionBtn_seePremium);
        }
    }


    @Click(R.id.actionBtn)
    protected void actionBtn() {
        switch (mPageState) {
            case Preview:
                Intent intent = new Intent(getActivity(), WizardActivity_.class);
                startActivity(intent);
                getActivity().finish();
                break;
            case ConnectedFree:
                TopUpFragment topUpFragment = new TopUpFragment_();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, topUpFragment).addToBackStack(null)
                        .commit();
                break;
            case ConnectedPremium:
                PlansFragment plansFragment = new PlansFragment_();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, plansFragment).addToBackStack(null)
                        .commit();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTagsRecycler = null;
    }
}

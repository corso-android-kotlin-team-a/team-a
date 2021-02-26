package com.ivanmorgillo.corsoandroid.teama.category

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ivanmorgillo.corsoandroid.teama.R
import com.ivanmorgillo.corsoandroid.teama.category.CategoryScreenAction.NavigateToRecipes
import com.ivanmorgillo.corsoandroid.teama.category.CategoryScreenAction.ShowInterruptedRequestMessage
import com.ivanmorgillo.corsoandroid.teama.category.CategoryScreenAction.ShowNoCategoryFoundMessage
import com.ivanmorgillo.corsoandroid.teama.category.CategoryScreenAction.ShowNoInternetMessage
import com.ivanmorgillo.corsoandroid.teama.category.CategoryScreenAction.ShowServerErrorMessage
import com.ivanmorgillo.corsoandroid.teama.category.CategoryScreenAction.ShowSlowInternetMessage
import com.ivanmorgillo.corsoandroid.teama.exhaustive
import com.ivanmorgillo.corsoandroid.teama.gone
import com.ivanmorgillo.corsoandroid.teama.showAlertDialog
import com.ivanmorgillo.corsoandroid.teama.visible
import kotlinx.android.synthetic.main.fragment_category.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class CategoryFragment : Fragment() {
    private val viewModel: CategoryViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val refresh: SwipeRefreshLayout = category_refresh
        refresh.setOnRefreshListener { // swipe to refresh
            viewModel.send(CategoryScreenEvent.OnReady)
        }
        val categoryCardAdapter = CategoryAdapter { item: CategoryUI, _: View ->
            viewModel.send(CategoryScreenEvent.OnCategoryClick(item))
        }

        category_list.adapter = categoryCardAdapter

        viewModel.states.observe(viewLifecycleOwner, { state ->
            when (state) {
                is CategoryScreenStates.Content -> {
                    category_list_progressBar.gone()
                    categoryCardAdapter.setCategories(state.categories)
                    refresh.isRefreshing = false
                }
                CategoryScreenStates.Error -> {
                    category_list_progressBar.gone()
                    refresh.isRefreshing = false
                }
                CategoryScreenStates.Loading -> {
                    category_list_progressBar.visible()
                }
            }
        })
        viewModel.actions.observe(viewLifecycleOwner,
            { action ->
                when (action) {
                    is NavigateToRecipes -> {
                        val directions =
                            CategoryFragmentDirections.actionCategoryFragmentToRecipeFragment(action.category.title)
                        findNavController().navigate(directions)
                    }
                    ShowNoInternetMessage -> showNoInternetMessage(view)
                    ShowInterruptedRequestMessage -> showInterruptedRequestMessage(view)
                    ShowSlowInternetMessage -> showNoInternetMessage(view)
                    ShowServerErrorMessage -> showServerErrorMessage(view)
                    ShowNoCategoryFoundMessage -> showNoCategoryFoundMessage(view)
                }.exhaustive
            })

        viewModel.send(CategoryScreenEvent.OnReady)
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.states.value == null) {
            viewModel.send(CategoryScreenEvent.OnReady)
        }
    }

    private fun showServerErrorMessage(view: View) {
        category_list_progressBar.gone()
        view.showAlertDialog(resources.getString(R.string.server_error_title),
            resources.getString(R.string.server_error_message),
            R.drawable.ic_error,
            resources.getString(R.string.retry),
            { viewModel.send(CategoryScreenEvent.OnReady) },
            "",
            {}
        )
    }

    private fun showInterruptedRequestMessage(view: View) {
        category_list_progressBar.gone()
        view.showAlertDialog(resources.getString(R.string.connection_lost_error_title),
            resources.getString(R.string.connection_lost_error_message),
            R.drawable.ic_wifi_off,
            resources.getString(R.string.network_settings),
            { startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) },
            resources.getString(R.string.retry),
            { viewModel.send(CategoryScreenEvent.OnReady) }
        )
    }

    private fun showNoInternetMessage(view: View) {
        category_list_progressBar.gone()
        view.showAlertDialog(resources.getString(R.string.no_internet_error_title),
            resources.getString(R.string.no_internet_error_message),
            R.drawable.ic_wifi_off,
            resources.getString(R.string.network_settings),
            { startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) },
            resources.getString(R.string.retry),
            {
                viewModel.send(CategoryScreenEvent.OnReady)
            }
        )
    }

    private fun showNoCategoryFoundMessage(view: View) {
        category_list_progressBar.gone()
        view.showAlertDialog(resources.getString(R.string.no_category_found_error_title),
            resources.getString(R.string.no_category_found_error_message),
            R.drawable.ic_sad_face,
            resources.getString(R.string.retry),
            { viewModel.send(CategoryScreenEvent.OnReady) },
            "",
            {}
        )
    }
}

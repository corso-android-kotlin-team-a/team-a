package com.ivanmorgillo.corsoandroid.teama.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.tabs.TabLayout
import com.ivanmorgillo.corsoandroid.teama.R
import com.ivanmorgillo.corsoandroid.teama.detail.DetailScreenViewHolder.IngredientInstructionListViewHolder
import com.ivanmorgillo.corsoandroid.teama.detail.DetailScreenViewHolder.TabLayoutViewHolder
import com.ivanmorgillo.corsoandroid.teama.detail.DetailScreenViewHolder.TitleViewHolder
import com.ivanmorgillo.corsoandroid.teama.detail.DetailScreenViewHolder.VideoViewHolder
import com.ivanmorgillo.corsoandroid.teama.exhaustive
import com.ivanmorgillo.corsoandroid.teama.gone
import com.ivanmorgillo.corsoandroid.teama.visible
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import timber.log.Timber

// gli oggetti dentro questa sealed li stiamo aggiungendo a seconda dell'ordine della nostra schermata
// io seguo un pò anche il discorso di ivan perchè la nostra schermata è diversa
sealed class DetailScreenItems {

    data class Title(val title: String) : DetailScreenItems()
    data class Video(val video: String, val image: String) : DetailScreenItems()
    data class IngredientsInstructionsList(val ingredients: List<IngredientUI>, val instruction: String) :
        DetailScreenItems()

    object TabLayout : DetailScreenItems()
}

private const val VIDEO_VIEWTYPE = 1
private const val IGNREDIENTSINSTRUCTIONS_VIEWTYPE = 2
private const val TITLE_VIEWTYPE = 3
private const val TABLAYOUT_VIEWTYPE = 4
private const val YOUTUBE_INDEX = 8

class DetailScreenAdapter(private val onIngredientsClick: () -> Unit, private val onInstructionsClick: () -> Unit) :
    RecyclerView.Adapter<DetailScreenViewHolder>() {
    var items: List<DetailScreenItems> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    /**In base all'elemento che stiamo utilizzando ci ritorna un intero
     * che rappresenta il viewType.
     * */
    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when (item) {
            is DetailScreenItems.Video -> VIDEO_VIEWTYPE
            is DetailScreenItems.IngredientsInstructionsList -> IGNREDIENTSINSTRUCTIONS_VIEWTYPE
            is DetailScreenItems.Title -> TITLE_VIEWTYPE
            is DetailScreenItems.TabLayout -> TABLAYOUT_VIEWTYPE
        }.exhaustive
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailScreenViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIDEO_VIEWTYPE -> {
                val view = layoutInflater.inflate(R.layout.detail_screen_video, parent, false)
                VideoViewHolder(view)
            }
            IGNREDIENTSINSTRUCTIONS_VIEWTYPE -> {
                val view = layoutInflater.inflate(R.layout.detail_ingredient_instruction, parent, false)
                IngredientInstructionListViewHolder(view)
            }
            TITLE_VIEWTYPE -> {
                val view = layoutInflater.inflate(R.layout.detail_screen_title, parent, false)
                TitleViewHolder(view)
            }
            TABLAYOUT_VIEWTYPE -> {
                val view = layoutInflater.inflate(R.layout.tab_button_details, parent, false)
                TabLayoutViewHolder(view)
            }
            else -> error("ViewTypeNotValid!")
        }
    }

    override fun onBindViewHolder(holder: DetailScreenViewHolder, position: Int) {
        when (holder) {
            is VideoViewHolder -> holder.bind(items[position] as DetailScreenItems.Video)
            is IngredientInstructionListViewHolder -> holder.bind(
                items[position] as DetailScreenItems.IngredientsInstructionsList
            )
            is TitleViewHolder -> holder.bind(items[position] as DetailScreenItems.Title)
            is TabLayoutViewHolder -> holder.bind(onIngredientsClick, onInstructionsClick)
        }
    }

    override fun getItemCount(): Int = items.size
}

sealed class DetailScreenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    // view holder per il titolo
    class TitleViewHolder(itemView: View) : DetailScreenViewHolder(itemView) {
        private val titleDetail = itemView.findViewById<TextView>(R.id.detail_screen_title)
        fun bind(title: DetailScreenItems.Title) {
            titleDetail.text = title.title
        }
    }

    class VideoViewHolder(itemView: View) : DetailScreenViewHolder(itemView) {
        private var startSeconds = 0f // secondi a cui far iniziare il video (0 = dall'inizio)
        fun bind(video: DetailScreenItems.Video) {
            val imageDetail = itemView.findViewById<ImageView>(R.id.detail_screen_image)
            val videoDetail = itemView.findViewById<YouTubePlayerView>(R.id.detail_screen_video)
            if (video.video.isEmpty()) { // se il video è vuoto (non esiste) mostra l'immagine
                imageDetail.load(video.image)
                imageDetail.visible()
                videoDetail.gone()
            } else { // altrimenti nasconde l'immagine e mostra il video
                videoDetail.visible()
                imageDetail.gone()
                videoDetail.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        // esempio video URL: https://www.youtube.com/watch?v=SQnr4Z-7rok
                        val videoId = video.video.substring(video.video.indexOf("watch?v=") + YOUTUBE_INDEX)
                        Timber.d("Sto caricando: https://www.youtube.com/watch?v=$videoId")
                        youTubePlayer.loadVideo(videoId, startSeconds)
                    }

                    override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                        super.onError(youTubePlayer, error)
                        imageDetail.load(video.image)
                        imageDetail.visible()
                        videoDetail.gone()
                    }
                })
            }
        }
    }

    class IngredientInstructionListViewHolder(itemView: View) : DetailScreenViewHolder(itemView) {

        private val ingredientDetail = itemView.findViewById<RecyclerView>(R.id.detail_screen_ingredient_list)
        private val instructionDetail = itemView.findViewById<TextView>(R.id.detail_screen_instruction)
        fun bind(ingredientInstructions: DetailScreenItems.IngredientsInstructionsList) {
            // questa striscia contiene una recyclerview quindi a questa lista serve:
            // - un adapter e una lista di elem da passare all'adapter.
            val adapter = ListIngredientAdapter()
            ingredientDetail.adapter = adapter
            adapter.setIngredients(ingredientInstructions.ingredients)

            instructionDetail.text = ingredientInstructions.instruction
        }
    }

    class TabLayoutViewHolder(itemView: View) : DetailScreenViewHolder(itemView) {

        private val tabLayout = itemView.findViewById<TabLayout>(R.id.tab_layout_detail)

        fun bind(onIngredientsClick: () -> Unit, onInstructionsClick: () -> Unit) {
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    Timber.d("OnTabSelected: ${tab.position}")

                    if (tab.position == 0) {
                        onIngredientsClick()
                    } else {
                        onInstructionsClick()
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) = Unit

                override fun onTabReselected(tab: TabLayout.Tab?) = Unit
            })
        }
    }
}

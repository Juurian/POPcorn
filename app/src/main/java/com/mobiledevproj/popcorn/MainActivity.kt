package com.mobiledevproj.popcorn

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobiledevproj.popcorn.ui.theme.POPcornTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import org.json.JSONArray
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import kotlinx.coroutines.MainScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import com.mobiledevproj.popcorn.sign_in.GoogleAuthUiClient

/*
The entry point for the POPcorn application.
It initializes the UI and handles navigation using Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val moviesViewModel: MoviesViewModel = viewModel()
            val favouritesViewModel by viewModels<FavouritesViewModel>()
            val watchlistViewModel by viewModels<WatchlistViewModel>()
            val context: Context = this
            val windowSize = calculateWindowSizeClass(activity = (this))

            getMovies { movies ->
                moviesViewModel.movies = movies
            }

            val onSignOut: () -> Unit = {
                navController.navigate("sign_in")
            }

            val isDarkModeEnabled by themeViewModel.isDarkModeEnabled.observeAsState(initial = false)

            // Can dynamically change configuration based on portrait or landscape view
            val configuration = LocalConfiguration.current

            AppNavigator(
                navController,
                favouritesViewModel,
                watchlistViewModel,
                context,
                windowSize.widthSizeClass,
                isDarkModeEnabled,
                onToggleDarkMode = { themeViewModel.toggleDarkMode() }
            )
        }
    }
}

/*
A View model to keep track of
the theme based on the state of the dark mode switch.
 */
class ThemeViewModel : ViewModel() {
    private val _isDarkModeEnabled = MutableLiveData<Boolean>(false)
    val isDarkModeEnabled: LiveData<Boolean> = _isDarkModeEnabled

    fun toggleDarkMode() {
        _isDarkModeEnabled.value = !_isDarkModeEnabled.value!!
    }
}

/*
Movie Page
Displays movie data in a list.
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoviePage(
    navController: NavHostController,
    moviesViewModel: MoviesViewModel,
    windowSize: WindowWidthSizeClass,
    isDarkModeEnabled: Boolean) {
    val movies = moviesViewModel.movies ?: emptyList()
    var query by remember { mutableStateOf("") }
    val filteredList = movies.filter {
        it.title.contains(query, ignoreCase = true)
    }
    val topBarContentColor = if (isDarkModeEnabled) Color.Black else Color.White

    // Can dynamically change configuration based on portrait or landscape view
    val currentOrientation = LocalConfiguration.current.orientation

    POPcornTheme(darkTheme = isDarkModeEnabled) {
        if(windowSize == WindowWidthSizeClass.Compact || windowSize == WindowWidthSizeClass.Medium && currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = topBarContentColor
                            )
                        }

                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.bodySmall,
                            color = topBarContentColor
                        )
                        Text(
                            text = "Movies",
                            style = MaterialTheme.typography.headlineMedium,
                            color = topBarContentColor,
                            modifier = Modifier.weight(1f).padding(start = 75.dp)
                        )
                    }
                },
                bottomBar = { POPcornBottomNavigation(navController) }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(65.dp))

                    SearchBar(Modifier.padding(horizontal = 0.dp)) { newQuery ->
                        query = newQuery
                    }
                    Spacer(Modifier.height(16.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        items(filteredList) { movie ->
                            MovieCard(movie, navController = navController)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        } else {
            PermanentNavigationDrawer(drawerContent = {
                PermanentDrawerSheet {
                    Text("POPcorn")
                    Divider()
                    NavigationDrawerItem(
                        label = {Text(stringResource(R.string.bottom_navigation_home))},
                        selected = false,
                        onClick = {navController.navigate("popcornPortrait")}
                    )
                    NavigationDrawerItem(
                        label = {Text(stringResource(R.string.bottom_navigation_profile))},
                        selected = false,
                        onClick = {navController.navigate("profilePage")}
                    )
                }
            }) {
                Scaffold(
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = "Back",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(65.dp))

                        SearchBar(Modifier.padding(horizontal = 0.dp)) { newQuery ->
                            query = newQuery
                        }
                        Spacer(Modifier.height(16.dp))
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 0.dp)
                        ) {
                            items(filteredList) { movie ->
                                MovieCard(movie, navController = navController)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

/*
Movie Card
Displays the movie image, description, and genre on a card.
*/
@Composable
fun MovieCard(movie: MoviesItem, modifier: Modifier = Modifier, navController: NavHostController) {
    val painter = rememberImagePainter(data = movie.images)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    navController.navigate("movieDetails/${movie.id}")
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Text(text = movie.title, style = MaterialTheme.typography.titleMedium)

                val genres = parseGenres(movie.genre)
                Text(text = genres, style = MaterialTheme.typography.bodyMedium)

                Text(text = movie.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// Function to parse and format Movie Genres
fun parseGenres(genresJson: String): String {
    try {
        val jsonArray = JSONArray(genresJson)
        val genreList = mutableListOf<String>()

        for (i in 0 until jsonArray.length()) {
            val genre = jsonArray.getString(i)
            genreList.add(genre)
        }

        return genreList.joinToString(", ")
    } catch (e: Exception) {
        return genresJson
    }
}

/*
Movie Details Page
Displays specific movie data including
movie image, title, description, rating, genre and year.
Includes a favourite button to add to favourites collection.
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetails(
    movie: MoviesItem,
    navController: NavHostController,
    favouritesViewModel: FavouritesViewModel,
    watchlistViewModel: WatchlistViewModel,
    windowSize: WindowWidthSizeClass,
    isDarkModeEnabled: Boolean
) {
    var isFavourite by remember { mutableStateOf(favouritesViewModel.isMovieInFavourites(movie)) }
    var isInWatchlist by remember { mutableStateOf(watchlistViewModel.isMovieInWatchlist(movie)) }
    var userRating by remember { mutableStateOf(0) }
    var fetchedRating by remember { mutableStateOf(0) }
    val topBarContentColor = if (isDarkModeEnabled) Color.Black else Color.White

    // Can dynamically change configuration based on portrait or landscape view
    val currentOrientation = LocalConfiguration.current.orientation

    LaunchedEffect(Unit) {
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val movieRef = firebaseDatabase.getReference("ratings/${movie.id}/$userId")

        movieRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fetchedRating = snapshot.getValue(Int::class.java) ?: 0
                userRating = fetchedRating
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
POPcornTheme(darkTheme = isDarkModeEnabled) {
    if(windowSize == WindowWidthSizeClass.Compact || windowSize == WindowWidthSizeClass.Medium && currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = topBarContentColor
                        )
                    }

                    Text(
                        text = "Back",
                        style = MaterialTheme.typography.bodySmall,
                        color = topBarContentColor
                    )
                }
            },
            bottomBar = { POPcornBottomNavigation(navController) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(50.dp))

                // Box for the image, favourites button, and watchlist button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(shape = RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Image(
                        painter = rememberImagePainter(data = movie.images),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Favourite icon
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Add to Favorites",
                        tint = if (isFavourite) Color.Red else Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .clickable {
                                val isCurrentlyFavourite =
                                    favouritesViewModel.isMovieInFavourites(movie)
                                if (isCurrentlyFavourite) {
                                    favouritesViewModel.removeMovieFromFavourites(movie)
                                } else {
                                    favouritesViewModel.addMovieToFavourites(movie)
                                }
                                isFavourite = !isCurrentlyFavourite // Toggle the state
                            }
                    )

                    // Watchlist icon
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Add to Watchlist",
                        tint = if (isInWatchlist) Color.Green else Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .clickable {
                                val isCurrentlyInWatchlist = watchlistViewModel.isMovieInWatchlist(movie)

                                if (isCurrentlyInWatchlist) {
                                    watchlistViewModel.removeFromWatchlist(movie)
                                } else {
                                    watchlistViewModel.addToWatchlist(movie)
                                }
                                isInWatchlist = !isCurrentlyInWatchlist // Toggle the state
                            }
                    )

                }

                Column(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = movie.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                pushStyle(style = SpanStyle(fontWeight = FontWeight.Bold))
                                append("Global Rating:")
                                pop()
                                append(" ${movie.rating / 2}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = buildAnnotatedString {
                                pushStyle(style = SpanStyle(fontWeight = FontWeight.Bold))
                                append("Year:")
                                pop()
                                append(" ${movie.year}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                pushStyle(style = SpanStyle(fontWeight = FontWeight.Bold))
                                append("User Rating:")
                                pop()
                                append(" $fetchedRating")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = buildAnnotatedString {
                                pushStyle(style = SpanStyle(fontWeight = FontWeight.Bold))
                                append("Genre:")
                                pop()
                                append(" ${parseGenres(movie.genre)}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Rating section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(5) { index ->
                            val isSelected = userRating > index
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isSelected) Color.Yellow else Color.Gray,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable {
                                        userRating = index + 1
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val firebaseDatabase = FirebaseDatabase.getInstance()
                            val userId = FirebaseAuth.getInstance().currentUser?.uid
                            val movieRef = firebaseDatabase.getReference("ratings/${movie.id}/$userId")

                            movieRef.setValue(userRating)
                                .addOnSuccessListener {
                                    // Rating saved successfully
                                    // You can update the UI or show a confirmation message here
                                }
                                .addOnFailureListener { e ->
                                    // Handle any errors that occurred while saving the rating
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Submit Rating")
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    } else {
        PermanentNavigationDrawer(drawerContent = {
            PermanentDrawerSheet {
                Text("POPcorn")
                Divider()
                NavigationDrawerItem(
                    label = {Text(stringResource(R.string.bottom_navigation_home))},
                    selected = false,
                    onClick = {navController.navigate("popcornPortrait")}
                )
                NavigationDrawerItem(
                    label = {Text(stringResource(R.string.bottom_navigation_profile))},
                    selected = false,
                    onClick = {navController.navigate("profilePage")}
                )
            }
        }) {
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(50.dp))

                    // Box for the image and the favourites button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(shape = RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Image(
                            painter = rememberImagePainter(data = movie.images),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Add to Favorites",
                            tint = if (isFavourite) Color.Red else Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clickable {
                                    val isCurrentlyFavourite =
                                        favouritesViewModel.isMovieInFavourites(movie)
                                    if (isCurrentlyFavourite) {
                                        favouritesViewModel.removeMovieFromFavourites(movie)
                                    } else {
                                        favouritesViewModel.addMovieToFavourites(movie)
                                    }
                                    isFavourite = !isCurrentlyFavourite // Toggle the state
                                }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = movie.title,
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = movie.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    pushStyle(style = SpanStyle(fontWeight = FontWeight.Bold))
                                    append("Global Rating:")
                                    pop()
                                    append(" ${movie.rating / 2}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = buildAnnotatedString {
                                    pushStyle(style = SpanStyle(fontWeight = FontWeight.Bold))
                                    append("Year:")
                                    pop()
                                    append(" ${movie.year}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    pushStyle(style = SpanStyle(fontWeight = FontWeight.Bold))
                                    append("User Rating:")
                                    pop()
                                    append(" $fetchedRating")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = buildAnnotatedString {
                                    pushStyle(style = SpanStyle(fontWeight = FontWeight.Bold))
                                    append("Genre:")
                                    pop()
                                    append(" ${parseGenres(movie.genre)}")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Rating section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(5) { index ->
                                val isSelected = userRating > index
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Yellow else Color.Gray,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            userRating = index + 1
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val firebaseDatabase = FirebaseDatabase.getInstance()
                                val userId = FirebaseAuth.getInstance().currentUser?.uid
                                val movieRef = firebaseDatabase.getReference("ratings/${movie.id}/$userId")

                                movieRef.setValue(userRating)
                                    .addOnSuccessListener {
                                        // Rating saved successfully
                                        // You can update the UI or show a confirmation message here
                                    }
                                    .addOnFailureListener { e ->
                                        // Handle any errors that occurred while saving the rating
                                    }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(vertical = 8.dp)
                        ) {
                            Text("Submit Rating")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
}

/*
Favourites View Model
Contains functions to add and remove favourite movies.
*/
class FavouritesViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val userId: String = auth.currentUser?.uid ?: ""
    private val favouritesRef: DatabaseReference = database.reference.child("users").child(userId).child("favorites")

    private val _favouriteMovies = MutableLiveData<List<MoviesItem>>(emptyList())
    val favouriteMovies: LiveData<List<MoviesItem>> = _favouriteMovies

    init {
        favouritesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val movies = mutableListOf<MoviesItem>()
                for (movieSnapshot in snapshot.children) {
                    val movie = movieSnapshot.getValue(MoviesItem::class.java)
                    movie?.let { movies.add(it) }
                }
                _favouriteMovies.value = movies
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    fun isMovieInFavourites(movie: MoviesItem): Boolean {
        return _favouriteMovies.value?.any { it.id == movie.id } ?: false
    }

    fun addMovieToFavourites(movie: MoviesItem) {
        favouritesRef.child(movie.id).setValue(movie)
    }

    fun removeMovieFromFavourites(movie: MoviesItem) {
        favouritesRef.child(movie.id).removeValue()
    }
}

/*
WatchlistViewModel
Contains functions to add and remove movies from a users watchlist.
 */
class WatchlistViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val userId: String = auth.currentUser?.uid ?: ""
    private val watchlistRef: DatabaseReference = database.reference.child("users").child(userId).child("watchlist")

    private val _watchlistMovies = MutableLiveData<List<MoviesItem>>(emptyList())
    val watchlistMovies: LiveData<List<MoviesItem>> = _watchlistMovies

    init {
        watchlistRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val movies = mutableListOf<MoviesItem>()
                for (movieSnapshot in snapshot.children) {
                    val movie = movieSnapshot.getValue(MoviesItem::class.java)
                    movie?.let { movies.add(it) }
                }
                _watchlistMovies.value = movies
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    fun isMovieInWatchlist(movie: MoviesItem): Boolean {
        return _watchlistMovies.value?.any { it.id == movie.id } ?: false
    }

    fun addToWatchlist(movie: MoviesItem) {
        watchlistRef.child(movie.id).setValue(movie)
    }

    fun removeFromWatchlist(movie: MoviesItem) {
        watchlistRef.child(movie.id).removeValue()
    }
}

/*
Social Page
Displays Users of the application that can be
added to their Connections.
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialPage(
    navController: NavHostController,
    windowSize: WindowWidthSizeClass,
    isDarkModeEnabled: Boolean) {
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance()
    val currentUser = auth.currentUser
    val topBarContentColor = if (isDarkModeEnabled) Color.Black else Color.White

    // Can dynamically change configuration based on portrait or landscape view
    val currentOrientation = LocalConfiguration.current.orientation
    val usersRef = database.reference.child("users")

    val userList = remember { mutableStateOf<List<User>>(emptyList()) }
    val searchQuery = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<User>()
                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: ""
                    val userName = userSnapshot.child("username").value.toString()
                    val user = User(userId, userName)
                    users.add(user)
                }
                userList.value = users
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    val addedUsers = remember { mutableSetOf<String>() }
    val addedUsersState = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(userList.value) {
        userList.value.forEach { user ->
            addedUsersState[user.id] = addedUsers.contains(user.id)
        }
    }

    fun addUserAsFriend(userId: String, isAdded: Boolean) {
        currentUser?.uid?.let { currentUserId ->
            val currentUserConnectionsRef = usersRef.child(currentUserId).child("connections")

            if (isAdded) {
                currentUserConnectionsRef.orderByValue().equalTo(userId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.children.forEach { it.ref.removeValue() }
                            addedUsers.remove(userId)
                            addedUsersState[userId] = false
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
            } else {
                currentUserConnectionsRef.push().setValue(userId)
                addedUsers.add(userId)
                addedUsersState[userId] = true
            }
        }
    }

    POPcornTheme(darkTheme = isDarkModeEnabled) {
        if(windowSize == WindowWidthSizeClass.Compact || windowSize == WindowWidthSizeClass.Medium && currentOrientation == Configuration.ORIENTATION_PORTRAIT){
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = topBarContentColor
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.back),
                            style = MaterialTheme.typography.bodySmall,
                            color = topBarContentColor
                        )
                        Text(
                            text = "Connections",
                            style = MaterialTheme.typography.headlineMedium,
                            color = topBarContentColor,
                            modifier = Modifier.weight(1f).padding(start = 46.dp)
                        )
                    }
                },
                bottomBar = { POPcornBottomNavigation(navController) }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(60.dp))

                    TextField(
                        value = searchQuery.value,
                        onValueChange = { searchQuery.value = it },
                        label = { Text(text = stringResource(id = R.string.searchforusers)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )

                    val filteredUsers = userList.value.filter {
                        it.name.contains(searchQuery.value, ignoreCase = true)
                    }

                    LazyColumn {
                        items(filteredUsers) { user ->
                            val added = addedUsersState[user.id] ?: false

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = user.name)
                                Spacer(modifier = Modifier.weight(1f))
                                Button(
                                    onClick = { addUserAsFriend(user.id, added) },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(if (added) stringResource(id = R.string.remove) else stringResource(id = R.string.add))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            PermanentNavigationDrawer(drawerContent = {
                PermanentDrawerSheet {
                    Text("POPcorn")
                    Divider()
                    NavigationDrawerItem(
                        label = {Text(stringResource(R.string.bottom_navigation_home))},
                        selected = false,
                        onClick = {navController.navigate("popcornPortrait")}
                    )
                    NavigationDrawerItem(
                        label = {Text(stringResource(R.string.bottom_navigation_profile))},
                        selected = false,
                        onClick = {navController.navigate("profilePage")}
                    )
                }
            }) {
                Scaffold(
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = "Back",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(Modifier.height(60.dp))

                        TextField(
                            value = searchQuery.value,
                            onValueChange = { searchQuery.value = it },
                            label = { Text("Search for users") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )

                        val filteredUsers = userList.value.filter {
                            it.name.contains(searchQuery.value, ignoreCase = true)
                        }

                        LazyColumn {
                            items(filteredUsers) { user ->
                                val added = addedUsersState[user.id] ?: false

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = user.name)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Button(
                                        onClick = { addUserAsFriend(user.id, added) },
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text(if (added) "Remove" else "Add")
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// User Data Class
data class User(
    val id: String,
    val name: String,
)

/*
Watchlist Page
Displays movies that the user has added to their watchlist.
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistPage(
    navController: NavHostController,
    watchlistViewModel: WatchlistViewModel,
    windowSize: WindowWidthSizeClass,
    isDarkModeEnabled: Boolean
) {
    val watchlistMovies: List<MoviesItem> by watchlistViewModel.watchlistMovies.observeAsState(emptyList())
    val topBarContentColor = if (isDarkModeEnabled) Color.Black else Color.White

    POPcornTheme(darkTheme = isDarkModeEnabled) {
        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = topBarContentColor
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.back),
                        style = MaterialTheme.typography.bodySmall,
                        color = topBarContentColor
                    )
                    Text(
                        text = stringResource(id = R.string.watchlist),
                        style = MaterialTheme.typography.headlineMedium,
                        color = topBarContentColor,
                        modifier = Modifier.weight(1f).padding(start = 60.dp)
                    )
                }
            },
            bottomBar = {
                POPcornBottomNavigation(navController)
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(65.dp))

                if (watchlistMovies.isEmpty()) {
                    Text(
                        text = "No movies in watchlist!",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        items(watchlistMovies) { movie ->
                            MovieCard(movie, navController = navController)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

/*
Profile Page
Displays user data and includes Sign out button.
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(
    navController: NavHostController,
    onSignOut: () -> Unit,
    windowSize: WindowWidthSizeClass,
    isDarkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit,
    textColor: Color
) {
    val firebaseAuth = FirebaseAuth.getInstance()
    val currentUser = firebaseAuth.currentUser
    val topBarContentColor = if (isDarkModeEnabled) Color.Black else Color.White

    // Can dynamically change configuration based on portrait or landscape view
    val currentOrientation = LocalConfiguration.current.orientation

    var username by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    // Fetch the username
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}")
            val dataSnapshot = userRef.get().await()

            if (dataSnapshot.exists()) {
                username = dataSnapshot.child("username").getValue(String::class.java) ?: ""
                firstName = dataSnapshot.child("firstName").getValue(String::class.java) ?: ""
                lastName = dataSnapshot.child("lastName").getValue(String::class.java) ?: ""
            }
        }
    }
    POPcornTheme(darkTheme = isDarkModeEnabled) {
        if(windowSize == WindowWidthSizeClass.Compact || windowSize == WindowWidthSizeClass.Medium && currentOrientation == Configuration.ORIENTATION_PORTRAIT){
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = topBarContentColor
                            )
                        }

                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.bodySmall,
                            color = topBarContentColor
                        )
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.headlineMedium,
                            color = topBarContentColor,
                            modifier = Modifier.weight(1f).padding(start = 80.dp)
                        )
                    }
                },
                bottomBar = { POPcornBottomNavigation(navController) }
            ) { padding ->
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(100.dp))
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.drive),
                                contentDescription = "Profile Icon",
                                modifier = Modifier
                                    .size(128.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.height(16.dp))

                            Text("Username: $username")
                            Text("First name: $firstName")
                            Text("Last name: $lastName")

                            Spacer(Modifier.height(20.dp))
                            Button(onClick = onSignOut) {
                                Text("Sign Out")
                            }

                            Spacer(Modifier.height(50.dp))

                            Text(
                                text = "Dark Mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                            DarkModeSwitch(
                                isDarkModeEnabled = isDarkModeEnabled,
                                onToggleDarkMode = onToggleDarkMode
                            )
                        }
                    }
                }
            }
        } else {
            PermanentNavigationDrawer(drawerContent = {
                PermanentDrawerSheet {
                    Text("POPcorn")
                    Divider()
                    NavigationDrawerItem(
                        label = {Text(stringResource(R.string.bottom_navigation_home))},
                        selected = false,
                        onClick = {navController.navigate("popcornPortrait")}
                    )
                    NavigationDrawerItem(
                        label = {Text(stringResource(R.string.bottom_navigation_profile))},
                        selected = false,
                        onClick = {navController.navigate("profilePage")}
                    )
                }
            }) {
                Scaffold(
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = "Back",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                ) { padding ->
                    Column(modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.drive),
                                    contentDescription = "Profile Icon",
                                    modifier = Modifier
                                        .size(128.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(Modifier.height(16.dp))

                                Text("Username: $username")
                                Text("First name: $firstName")
                                Text("Last name: $lastName")

                                Spacer(Modifier.height(16.dp))
                                Button(onClick = onSignOut) {
                                    Text("Sign Out")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/*
A Dark mode switch that allows users to
switch the theme to dark mode.
 */
@Composable
fun DarkModeSwitch(
    isDarkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val thumbColor = if (isDarkModeEnabled) Color.White else Color.Gray
    val trackColor = if (isDarkModeEnabled) Color.DarkGray else Color.LightGray

    Switch(
        checked = isDarkModeEnabled,
        onCheckedChange = { onToggleDarkMode() }, // Update the state when the switch changes
        colors = SwitchDefaults.colors(
            checkedThumbColor = thumbColor,
            checkedTrackColor = trackColor,
            uncheckedThumbColor = thumbColor,
            uncheckedTrackColor = trackColor
        ),
        modifier = Modifier.size(48.dp, 24.dp)
    )
}

/*
App Navigator
Facilitates navigation within the application.
*/
@Composable
fun AppNavigator(
    navController: NavHostController,
    favouritesViewModel: FavouritesViewModel,
    watchlistViewModel: WatchlistViewModel,
    context: Context,
    windowSize: WindowWidthSizeClass,
    isDarkModeEnabled: Boolean,
    onToggleDarkMode: () -> Unit
) {
    POPcornTheme(darkTheme = isDarkModeEnabled) {
        val moviesViewModel: MoviesViewModel = viewModel()

        NavHost(navController, startDestination = "popcornPortrait") {
            composable("popcornPortrait") {
                POPcornPortrait(navController, favouritesViewModel, windowSize, isDarkModeEnabled = isDarkModeEnabled)
            }
            composable("moviePage") {
                MoviePage(navController, moviesViewModel, windowSize, isDarkModeEnabled = isDarkModeEnabled)
            }
            composable("movieDetails/{movieId}") { backStackEntry ->
                val movieId = backStackEntry.arguments?.getString("movieId")
                val selectedMovie = moviesViewModel.movies?.find { it.id == movieId }

                if (selectedMovie != null) {
                    MovieDetails(
                        selectedMovie,
                        navController,
                        favouritesViewModel,
                        watchlistViewModel,
                        windowSize,
                        isDarkModeEnabled = isDarkModeEnabled
                    )
                } else {
                    Text("Movie not found")
                }
            }
            composable("socialPage") {
                SocialPage(navController, windowSize, isDarkModeEnabled = isDarkModeEnabled)
            }
            composable("watchlistPage") {
                WatchlistPage(navController, watchlistViewModel, windowSize, isDarkModeEnabled = isDarkModeEnabled)
            }
            composable("profilePage") {
                ProfilePage(navController, onSignOut = {
                    val intent = Intent(context, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                }, windowSize, isDarkModeEnabled = isDarkModeEnabled, onToggleDarkMode, textColor = Color.Black)
            }
        }
    }
}

// Preview Composable
@Preview(showBackground = true, device = "id:Nexus One", showSystemUi = true)
@Composable
fun GreetingPreview() {
    POPcornTheme {
        Greeting("Android")
    }
}

// Preview Composable
@Preview(showBackground = true, device = "id:Nexus One", showSystemUi = true)
@Composable
fun GreetingPreviewDark() {
    POPcornTheme(darkTheme = true) {
        Greeting("Android")
    }
}

// Greeting Composable
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier){
    Surface {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.mipmap.popcorn_logo_foreground), // Use your image name here
                contentDescription = null, // Provide a content description if needed
                modifier = Modifier.size(128.dp) // Adjust the size as needed
            )
            Text(
                text = "Hello $name, welcome to POPCorn!",
                modifier = modifier
            )
        }
    }
}

/*
Get Movies
Function to acquire movie data from API.
*/
private val mainScope = MainScope()

fun getMovies(callback: (Movies) -> Unit) {
    val client = OkHttpClient()

    val request = Request.Builder()
        .url("https://imdb-top-100-movies1.p.rapidapi.com/")
        .get()
        .addHeader("X-RapidAPI-Key", "980648891cmsh4d49ee8f2888ad9p1dc229jsn2b550bba1d65")
        .addHeader("X-RapidAPI-Host", "imdb-top-100-movies1.p.rapidapi.com")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                val jsonString = response.body!!.string()
                val jsonArray = JSONArray(jsonString)
                val movies = Movies()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val id = jsonObject.getString("id")
                    val title = jsonObject.getString("title")
                    val description = jsonObject.getString("description")
                    val link = jsonObject.getString("link")
                    val genre = jsonObject.getString("genre")
                    val images = jsonObject.getJSONArray("images").getJSONArray(0).getString(1)
                    val rating = jsonObject.getDouble("rating")
                    val year = jsonObject.getString("year")

                    val movieItem = MoviesItem(id, title, description, link, genre, images, rating, year)
                    movies.add(movieItem)
                }

                mainScope.launch(Dispatchers.Main) {
                    callback(movies)
                }
            }
        }
    })
}

/*
Search Bar Composable
Allows search functionality within the application.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = LocalFocusManager.current

    TextField(
        value = query,
        onValueChange = {
            query = it
            onQueryChange(it)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        colors = TextFieldDefaults.textFieldColors(
            cursorColor = Color.Black,
            unfocusedPlaceholderColor = Color.Gray
        ),
        placeholder = {
            Text(stringResource(R.string.placeholder_search))
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        singleLine = true,// Assign the focusRequester
        keyboardActions = KeyboardActions(
            onSearch = {
                // val movies = fetchUpcomingMovies(query)
                // Do something with the list of movies
                // movies.toString()

                // Request focus on another element to dismiss the keyboDard
                focusRequester.clearFocus()
            }
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        )
    )
}

//Preview Composable
//@Preview(showBackground = true, backgroundColor = 0xFFF5F0EE)
//@Composable
//fun SearchBarPreview() {
//    POPcornTheme { SearchBar(Modifier.padding(8.dp)) }
//}

/*
POPcorn Element
Dashboard element for Movie and Social Pages.
 */
@Composable
fun POPcornElement(
    @DrawableRes drawable: Int,
    @StringRes text: Int,
    destination: String,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable {
            navController.navigate(destination)
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
        )
        Text(
            text = stringResource(text),
            modifier = Modifier.paddingFromBaseline(top = 24.dp, bottom = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// Preview Composable
@Preview(showBackground = true, backgroundColor = 0xFFF5F0EE)
@Composable
fun POPcornElementPreview() {
    val navController = rememberNavController()
    POPcornTheme {
        POPcornElement(
            text = R.string.movies,
            drawable = R.drawable.movie,
            navController = navController,
            destination = "moviePage",
            modifier = Modifier.padding(8.dp)
        )
    }
}

/*
Dashboard Row
Displays homepage POPcorn element in row format.
 */
//@Composable
//fun DashboardRow(navController: NavHostController, modifier: Modifier = Modifier) {
//    LazyRow(
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        contentPadding = PaddingValues(horizontal = 16.dp),
//        modifier = modifier
//    ) {
//        items(dashboardData) { item ->
//            val destination = if (item.text == R.string.movies) "moviePage" else "socialPage"
//            POPcornElement(item.drawable, item.text, destination, navController)
//        }
//    }
//}

@Composable
fun DashboardRow(navController: NavHostController, modifier: Modifier = Modifier) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier
    ) {
        items(dashboardData) { item ->
            val destination = when (item.text) {
                R.string.movies -> "moviePage"
                R.string.social -> "socialPage"
                R.string.watchlist -> "watchlistPage"
                else -> "defaultDestination" // You can set a default destination or handle other cases
            }
            POPcornElement(item.drawable, item.text, destination, navController)
        }
    }
}

/*
Home Section
Displays UI elements onto the Home Page.
*/
@Composable
fun HomeSection(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .paddingFromBaseline(top = 40.dp, bottom = 16.dp)
                .padding(horizontal = 16.dp)
        )
        content()
    }
}

// Preview Composable
@Preview(showBackground = true, backgroundColor = 0xFFF5F0EE)
@Composable
fun HomeSectionPreview() {
    val navController = rememberNavController()
    POPcornTheme {
        HomeSection(R.string.Dashboard) {
            DashboardRow(navController = navController)
        }
    }
}

/*
Favourite Collection Card
Displays Favourite Movie data in a card element.
*/
@Composable
fun FavouriteCollectionCard(
    movie: MoviesItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                Image(
                    painter = rememberImagePainter(data = movie.images),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = movie.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/*
Favourite Collection Grid
Displays Favourite Collection Card element in Grid format.
*/
@Composable
fun FavouriteCollectionsGrid(
    favouriteMovies: List<MoviesItem>,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
        LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.height(500.dp)
    )  {
        items(favouriteMovies) { movie ->
            FavouriteCollectionCard(
                movie = movie,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f),
                onClick = {
                    navController.navigate("movieDetails/${movie.id}")
                }
            )
        }
    }
}

/*
Home Screen
Displays the Home Page elements.
*/
@Composable
fun HomeScreen(
    navController: NavHostController,
    favouritesViewModel: FavouritesViewModel,
    modifier: Modifier = Modifier
) {
    val favouriteMovies by favouritesViewModel.favouriteMovies.observeAsState(emptyList())

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp)
    ) {
        HomeSection(title = R.string.Dashboard) {
            DashboardRow(navController)
        }
        HomeSection(title = R.string.favourite_collection) {
            if (favouriteMovies.isNotEmpty()) {
                FavouriteCollectionsGrid(favouriteMovies, navController)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Text("No movies yet!")
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// Preview Composable
@Preview(showBackground = true, backgroundColor = 0xFFF5F0EE, heightDp = 180)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    POPcornTheme { HomeScreen(navController, favouritesViewModel = FavouritesViewModel()) }
}

/*
Bottom Navigation of the application.
*/
@Composable
private fun POPcornBottomNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null
                )
            },
            label = {
                Text(stringResource(R.string.bottom_navigation_home))
            },
            selected = true,
            onClick = {
                navController.navigate("popcornPortrait")
            }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null
                )
            },
            label = {
                Text(stringResource(R.string.bottom_navigation_profile))
            },
            selected = false,
            onClick = {
                navController.navigate("profilePage")
            }
        )
    }
}

// Preview Composable
@Preview(showBackground = true, backgroundColor = 0xFFF5F0EE)
@Composable
fun POPcornBottomNavigationPreview() {
    val navController = rememberNavController()
    POPcornTheme { POPcornBottomNavigation(navController, Modifier.padding(top = 24.dp)) }
}

/*
POPcorn Portrait
Displays main UI elements on the Home Page.
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POPcornPortrait(
    navController: NavHostController,
    favouritesViewModel: FavouritesViewModel,
    windowSize: WindowWidthSizeClass,
    isDarkModeEnabled: Boolean
){
    val firebaseAuth = FirebaseAuth.getInstance()
    val currentUser = firebaseAuth.currentUser
    val topBarContentColor = if (isDarkModeEnabled) Color.Black else Color.White

    var username by remember { mutableStateOf("") }

    // Fetch the username
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            val userRef = FirebaseDatabase.getInstance().getReference("users/${currentUser.uid}/username")
            val dataSnapshot = userRef.get().await()

            if (dataSnapshot.exists()) {
                username = dataSnapshot.getValue(String::class.java) ?: ""
            }
        }
    }

    POPcornTheme(darkTheme = isDarkModeEnabled) {
        if(windowSize == WindowWidthSizeClass.Compact || windowSize == WindowWidthSizeClass.Medium){
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Welcome, $username",
                            style = MaterialTheme.typography.headlineMedium,
                            color = topBarContentColor,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                },
                bottomBar = { POPcornBottomNavigation(navController) }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxSize()
                ) {
                    HomeScreen(navController, favouritesViewModel)
                }
            }
        } else {
            PermanentNavigationDrawer(drawerContent = {
                PermanentDrawerSheet {
                    Text("POPcorn")
                    Divider()
                    NavigationDrawerItem(
                        label = {Text(stringResource(R.string.bottom_navigation_home))},
                        selected = false,
                        onClick = {navController.navigate("popcornPortrait")}
                    )
                    NavigationDrawerItem(
                        label = {Text(stringResource(R.string.bottom_navigation_profile))},
                        selected = false,
                        onClick = {navController.navigate("profilePage")}
                    )
                }
            }) {
                Scaffold(
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Welcome, $username",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxSize()
                    ) {
                        HomeScreen(navController, favouritesViewModel)
                    }
                }
            }
        }
    }
}

// Preview Composable
//@Preview(widthDp = 360, heightDp = 640)
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun POPcornPortraitPreview(favouritesViewModel: FavouritesViewModel, isDarkModeEnabled: Boolean) {
    val navController = rememberNavController()
    POPcornPortrait(navController, favouritesViewModel, WindowWidthSizeClass.Expanded, isDarkModeEnabled)
}

// Dashboard Items
data class DashboardData(@DrawableRes val drawable: Int, @StringRes val text: Int)

val dashboardData = listOf(
    DashboardData(R.drawable.movie, R.string.movies),
    DashboardData(R.drawable.watchlist, R.string.watchlist),
    DashboardData(R.drawable.social, R.string.social)
)

// Favourite Movie Collection Data

private val favouriteCollectionsData = listOf(
    R.drawable.blade_runner to R.string.fc1_blade_runner,
    R.drawable.meg to R.string.fc2_meg,
    R.drawable.avengers to R.string.fc3_avengers,
    R.drawable.dark_knight to R.string.fc4_dark_knight
).map { DrawableStringPair(it.first, it.second) }

private data class DrawableStringPair(
    @DrawableRes val drawable: Int,
    @StringRes val text: Int
)
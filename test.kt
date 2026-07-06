import it.fast4x.innertube.YtMusic
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val playlistId = "OLAK5uy_lHyXXV-KsYaMKjdr7k4Ow4nd5zhrhoSG0"
    val result = YtMusic.getAlbumSongs(playlistId)
    println("Songs count: ${result.getOrNull()?.size}")
    result.getOrNull()?.forEachIndexed { index, song ->
        println("Index $index: ${song.info.name}")
    }
}

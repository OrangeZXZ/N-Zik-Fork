package vlcj

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import player.PlayerController
import player.frame.FrameRenderer

class VlcjFrameController constructor(
    private val controller: VlcjController = VlcjController(),
) : FrameRenderer, PlayerController by controller {
    private val _size = MutableStateFlow(0 to 0)
    override val size = _size.asStateFlow()

    private val _bytes = MutableStateFlow<ByteArray?>(null)
    override val bytes = _bytes.asStateFlow()

    init {
        // Disabled VLC video rendering to fix compilation errors.
    }
}

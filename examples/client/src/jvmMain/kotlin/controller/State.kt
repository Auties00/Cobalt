package controller

sealed class State {
    object Loading : State()
    data class Home(val messageCounter: Int, val recentProgress: Int?, val historyProgress: Int?) : State()
    data class Login(val qr: ByteArray) : State() {
        override fun equals(other: Any?): Boolean
                = this === other || (other is Login && qr.contentEquals(other.qr))
        override fun hashCode(): Int
                = qr.contentHashCode()
    }
}
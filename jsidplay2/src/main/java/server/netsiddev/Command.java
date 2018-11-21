package server.netsiddev;

/** Available commands to clients */
public enum Command {
	/* 0 */
	FLUSH, TRY_SET_SID_COUNT, MUTE, TRY_RESET,

	/* 4 */
	TRY_DELAY, TRY_WRITE, TRY_READ, GET_VERSION,

	/* 8 */
	TRY_SET_SAMPLING, TRY_SET_CLOCKING, GET_CONFIG_COUNT, GET_CONFIG_INFO,

	/* 12 */
	SET_SID_POSITION, SET_SID_LEVEL, TRY_SET_SID_MODEL, SET_DELAY
}
package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.IOUtils;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.Configuration;
import ui.entities.config.FavoritesSection;

@SuppressWarnings("serial")
public class FavoritesServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.FavoritesServletParameters")
	public static class FavoritesServletParameters {

		private Integer favoritesNumber = 0;

		public Integer getFavoritesNumber() {
			return favoritesNumber;
		}

		@Parameter(names = { "--favoritesNumber" }, descriptionKey = "FAVORITES_NUMBER", order = -2)
		public void setFavoritesNumber(Integer favoritesNumber) {
			this.favoritesNumber = favoritesNumber;
		}

	}

	public static final String FAVORITES_PATH = "/favorites";

	public FavoritesServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + FAVORITES_PATH;
	}

	@Override
	public boolean isSecured() {
		return true;
	}

	/**
	 * Get contents of the first SID favorites tab.
	 *
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/favorites
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final FavoritesServletParameters servletParameters = new FavoritesServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getServletPath());

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			final Integer favoritesNumber = servletParameters.getFavoritesNumber();

			if (favoritesNumber == configuration.getFavorites().size()) {
				// Small hack to add entries (only supported in C64 Jukebox)
				String specials = "[{\"filename\":\"imagination_thek_5b2sid_85805d.sid\",\"itemId\":\"219210\",\"categoryId\":4},{\"filename\":\"mutetus_gunnaro.sid\",\"itemId\":\"219215\",\"categoryId\":4},{\"filename\":\"mutetus_balancekeeper.sid\",\"itemId\":\"218471\",\"categoryId\":4},{\"filename\":\"belfast_child.sid\",\"itemId\":\"217204\",\"categoryId\":4},{\"filename\":\"you_2sid_8580.sid\",\"itemId\":\"213177\",\"categoryId\":4},{\"filename\":\"jump_thek_5b2sid65815d.sid\",\"itemId\":\"220528\",\"categoryId\":4},{\"filename\":\"mutetus_overthemoon.sid\",\"itemId\":\"220067\",\"categoryId\":4},{\"filename\":\"mutetus_table_scraps.sid\",\"itemId\":\"220224\",\"categoryId\":4},{\"filename\":\"mutetus_banaaninalle.sid\",\"itemId\":\"209406\",\"categoryId\":4},{\"filename\":\"mutetus_forestadventure.sid\",\"itemId\":\"204220\",\"categoryId\":4},{\"filename\":\"mutetus_only299.99.sid\",\"itemId\":\"203396\",\"categoryId\":4},{\"filename\":\"the_required_fields.sid\",\"itemId\":\"205479\",\"categoryId\":4},{\"filename\":\"playa_de_los_gatos_5b85805d.sid\",\"itemId\":\"207902\",\"categoryId\":4},{\"filename\":\"wave_without_a_shore.sid\",\"itemId\":\"203328\",\"categoryId\":4},{\"filename\":\"high-speed-chase.sid\",\"itemId\":\"205394\",\"categoryId\":4},{\"filename\":\"sun_and_the_moon.sid\",\"itemId\":\"200637\",\"categoryId\":4},{\"filename\":\"luma.sid\",\"itemId\":\"214106\",\"categoryId\":4},{\"filename\":\"cheezzytop.sid\",\"itemId\":\"221138\",\"categoryId\":4},{\"filename\":\"deepdive_ald.sid\",\"itemId\":\"221491\",\"categoryId\":4},{\"filename\":\"antarctic_burial.sid\",\"itemId\":\"185997\",\"categoryId\":4},{\"filename\":\"badass28laidbackversion29.sid\",\"itemId\":\"197529\",\"categoryId\":4},{\"filename\":\"bowlingballshuffle286581flag29.sid\",\"itemId\":\"191964\",\"categoryId\":4},{\"filename\":\"chinesehappypeople.sid\",\"itemId\":\"190659\",\"categoryId\":4},{\"filename\":\"Laxity - Junior.sid\",\"itemId\":\"192504\",\"categoryId\":4},{\"filename\":\"Narciso - Human Isolation.sid\",\"itemId\":\"192504\",\"categoryId\":4},{\"filename\":\"Zardax - HousesOfRose.sid\",\"itemId\":\"192504\",\"categoryId\":4},{\"filename\":\"mythus-dreadfulwaste.sid\",\"itemId\":\"188081\",\"categoryId\":4},{\"filename\":\"dynamite.sid\",\"itemId\":\"192854\",\"categoryId\":4},{\"filename\":\"electric_city_2sid_288580r52b6581ar4_sidfx29.sid\",\"itemId\":\"189742\",\"categoryId\":4},{\"filename\":\"elusive_groove.sid\",\"itemId\":\"187297\",\"categoryId\":4},{\"filename\":\"eoroid2020.sid\",\"itemId\":\"188541\",\"categoryId\":4},{\"filename\":\"honey.sid\",\"itemId\":\"197862\",\"categoryId\":4},{\"filename\":\"lullabyforteddybear.sid\",\"itemId\":\"193151\",\"categoryId\":4},{\"filename\":\"sad_song_2sid_8580_stereo.sid\",\"itemId\":\"193662\",\"categoryId\":4},{\"filename\":\"sograinyithertz_25hz.sid\",\"itemId\":\"193534\",\"categoryId\":4},{\"filename\":\"mythusstarkiller.sid\",\"itemId\":\"191164\",\"categoryId\":4},{\"filename\":\"turrican_rotm.sid\",\"itemId\":\"189430\",\"categoryId\":4},{\"filename\":\"wakeupcall.sid\",\"itemId\":\"189668\",\"categoryId\":4},{\"filename\":\"toggle_-_wintry_haze_8580.sid\",\"itemId\":\"188346\",\"categoryId\":4},{\"filename\":\"8bitwarrior.sid\",\"itemId\":\"224990\",\"categoryId\":4},{\"filename\":\"anoushka.sid\",\"itemId\":\"224996\",\"categoryId\":4},{\"filename\":\"burnout.sid\",\"itemId\":\"224995\",\"categoryId\":4},{\"filename\":\"mutetus_zoo_jam.sid\",\"itemId\":\"224997\",\"categoryId\":4},{\"filename\":\"Music/SMCLaxityThriller.sid\",\"itemId\":\"225023\",\"categoryId\":1},{\"filename\":\"thunderstruck.sid\",\"itemId\":\"212534\",\"categoryId\":4},{\"filename\":\"jurassic-park.sid\",\"itemId\":\"211405\",\"categoryId\":4},{\"filename\":\"sids_in_america.sid\",\"itemId\":\"209966\",\"categoryId\":4},{\"filename\":\"chronotrigger-battle.sid\",\"itemId\":\"202401\",\"categoryId\":4},{\"filename\":\"ultimate_axel_f.sid\",\"itemId\":\"228585\",\"categoryId\":4},{\"filename\":\"dkc2_locjaws_saga.sid\",\"itemId\":\"228459\",\"categoryId\":4},{\"filename\":\"r-typeamigato2sid.sid\",\"itemId\":\"228298\",\"categoryId\":4},{\"filename\":\"dkc_aquatic_ambience.sid\",\"itemId\":\"228090\",\"categoryId\":4},{\"filename\":\"captain_future_[2sid].sid\",\"itemId\":\"229451\",\"categoryId\":4},{\"filename\":\"The_Real_Ghostbusters.sid\",\"itemId\":\"228963\",\"categoryId\":4},{\"filename\":\"Crocketts_Theme.sid\",\"itemId\":\"228871\",\"categoryId\":4},{\"filename\":\"brothers_in_oscillators_5b85805d.sid\",\"itemId\":\"232924\",\"categoryId\":4},{\"filename\":\"Destination_Funktown.sid\",\"itemId\":\"1799355538\",\"categoryId\":18},{\"filename\":\"Machine_Yearning_2SID.sid\",\"itemId\":\"231320\",\"categoryId\":4},{\"filename\":\"lift_off_v2.sid\",\"itemId\":\"217592\",\"categoryId\":4},{\"filename\":\"jzd_bahamamas.sid\",\"itemId\":\"225052\",\"categoryId\":4},{\"filename\":\"Stacking_Firewood.sid\",\"itemId\":\"2858080782\",\"categoryId\":18},{\"filename\":\"Peace_Piece.sid\",\"itemId\":\"668406594\",\"categoryId\":18},{\"filename\":\"cocktailhour.sid\",\"itemId\":\"233361\",\"categoryId\":4},{\"filename\":\"turok2-cavestage.sid\",\"itemId\":\"233778\",\"categoryId\":4},{\"filename\":\"desertbus.sid\",\"itemId\":\"235305\",\"categoryId\":4},{\"filename\":\"dead_lock_2sid.sid\",\"itemId\":\"235538\",\"categoryId\":4},{\"filename\":\"dj_space_scatman.sid\",\"itemId\":\"233825\",\"categoryId\":4}]";
				setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(specials));
			} else {
				List<String> favoritesFilenames = getFavoritesByNumber(favoritesNumber);

				setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(favoritesFilenames));
			}

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private List<String> getFavoritesByNumber(Integer favoritesNumber) {
		List<String> filters = configuration.getFavorites().stream()
				.filter(favorites -> configuration.getFavorites().indexOf(favorites) == favoritesNumber).findFirst()
				.map(FavoritesSection::getFavorites).orElseGet(Collections::emptyList).stream()
				.map(this::getFavoritesFilename).collect(Collectors.toList());
		return filters;
	}

	private String getFavoritesFilename(HVSCEntry entry) {
		if (IOUtils.getFiles(entry.getPath(), configuration.getSidplay2Section().getHvsc(), null).size() > 0) {
			return C64_MUSIC + entry.getPath();
		} else if (IOUtils.getFiles(entry.getPath(), configuration.getSidplay2Section().getCgsc(), null).size() > 0) {
			return CGSC + entry.getPath();
		}
		return null;
	}

}

package net.thesilkminer.utilities.htmlcreator;

import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;

/**
 * A version used to identify the specific HTML used.
 *
 * @author TheSilkMiner
 * @since 1.0
 */
@SuppressWarnings("SpellCheckingInspection")
public enum HtmlVersion {
	HTML_5 {
		@Nullable
		@Override
		public String dtd() {
			return null;
		}
	},
	HTML_4_01_STRICT {
		@Nullable
		@Override
		public String dtd() {
			return "\"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\"";
		}

		@Override
		public boolean isV4() {
			return true;
		}
	},
	HTML_4_01_TRANSITIONAL {
		@Nullable
		@Override
		public String dtd() {
			return "\"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\"";
		}

		@Override
		public boolean isV4() {
			return true;
		}
	},
	HTML_4_01_FRAMESET {
		@Nullable
		@Override
		public String dtd() {
			return "\"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\"";
		}

		@Override
		public boolean isV4() {
			return true;
		}
	},
	XHTML_1_0_STRICT {
		@Nullable
		@Override
		public String dtd() {
			return "\"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"";
		}
	},
	XHTML_1_0_TRANSITIONAL {
		@Nullable
		@Override
		public String dtd() {
			return "\"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"";
		}
	},
	XHTML_1_0_FRAMESET {
		@Nullable
		@Override
		public String dtd() {
			return "\"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\"";
		}
	},
	XHTML_1_1 {
		@Nullable
		@Override
		public String dtd() {
			return "\"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\"";
		}
	};

	@Contract(pure = true)
	@Nullable
	public abstract String dtd();

	@Contract(pure = true)
	public boolean isV4() {
		return false;
	}
}

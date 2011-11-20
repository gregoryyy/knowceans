/*
 * Copyright (c) 2005-6 Gregor Heinrich. All rights reserved. Redistribution and
 * use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source
 * code must retain the above copyright notice, this list of conditions and the
 * following disclaimer. 2. Redistributions in binary form must reproduce the
 * above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESSED OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.knowceans.corpus.refactor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

/*
 * Created on Mar 22, 2005
 */

/**
 * IptcCategories represents the IPTC document categorisation hierarchy.
 * <p>
 * refactored class from knowceans.corpus.base and freshmind
 * 
 * @author gregor heinrich (gregor :: arbylon . net)
 */
public class IptcCategories implements ICategories, Serializable {

	/**
     *
     */
	private static final long serialVersionUID = 1L;

	HashMap<Integer, String> iptcMap = null;

	Vector<Integer> iptcIndex = null;

	public static void main(String[] args) {
		IptcCategories c = new IptcCategories();

		String test = "12005000";
		Vector<Integer> ints = c.subjects(test);
		String a = c.decode(ints);
		System.out.println(a);
		int[] bb = c.indexToHierarchy(c.iptcToIndex(4016038), true);
		for (int i : bb) {
			int aa = c.indexToIptc(i);
			System.out.println(i + " " + aa + " " + c.decode(aa));
		}
		System.out.println("XXXXXXXXXXXXXXX");
		bb = c.indexToSubindices(c.iptcToIndex(15010000), 1, true);
		System.out.println(c.decode(15010000));
		for (int i : bb) {
			int aa = c.indexToIptc(i);
			System.out.println(i + " " + aa + " " + c.decode(aa));
		}

	}

	public IptcCategories() {
		iptcMap = new HashMap<Integer, String>();
		iptcIndex = new Vector<Integer>();
		for (Object[] a : iptcCodes) {
			iptcMap.put((Integer) a[0], (String) a[1]);
			iptcIndex.add((Integer) a[0]);
		}
	}

	public Vector<Integer> subjects(String sub) {
		Vector<Integer> classes = new Vector<Integer>();
		StringTokenizer st = new StringTokenizer(sub, " ,;");
		while (st.hasMoreElements()) {
			String x = (String) st.nextElement();

			int code = Integer.parseInt(x);
			if (iptcMap.containsKey(code)) {
				classes.add(code);
			}
		}
		return classes;
	}

	public String decode(Vector<Integer> a) {
		StringBuffer x = new StringBuffer();
		for (int i : a) {
			x.append("[").append(iptcMap.get(i)).append("]");
		}
		return x.toString();
	}

	/**
	 * get description string for IPTC code
	 * 
	 * @param iptcCode
	 * @return
	 */
	public String decode(int iptcCode) {
		return iptcMap.get(iptcCode);
	}

	/**
	 * get the index of the IPTC category code
	 * 
	 * @param iptc code
	 * @return
	 */
	public int iptcToIndex(int iptc) {
		return iptcIndex.indexOf(iptc);
	}

	/**
	 * @return
	 */
	public int iptcCategoryCount() {
		return iptcIndex.size();
	}

	/**
	 * get the IPTC category code of the index
	 * 
	 * @param index code
	 * @return
	 */
	public int indexToIptc(int index) {
		return iptcIndex.get(index);
	}

	/**
	 * returns the index of the argument and those indices that correspond to
	 * the superclasses in the IPTC hierarchy.
	 * 
	 * @param index
	 * @return
	 */
	public int[] indexToHierarchy(int index, boolean include) {
		int iptc = indexToIptc(index);
		int level = getLevel(iptc);
		if (level == 0)
			return new int[0];
		int[] a = new int[include ? level : level - 1];
		for (int i = 0; i < a.length; i++) {
			int aindex = getLevelIptc(iptc, i);
			a[i] = iptcToIndex(aindex);
		}
		return a;
	}

	/**
	 * returns the indices of the direct subcategories of the index.
	 * 
	 * @param index of category (root not yet implemented)
	 * @param levels that should be considered
	 * @param include the argument.
	 * @return
	 */
	public int[] indexToSubindices(int index, int levels, boolean include) {

		int iptc = indexToIptc(index);
		int level = getLevel(iptc);
		Vector<Integer> a = new Vector<Integer>();
		// subcategories are those that follow the argument index
		// and have a higher level.
		if (include)
			a.add(index);
		while (true) {
			index++;
			int nextIptc = indexToIptc(index);
			int sublevel = getLevel(nextIptc);
			if (level >= sublevel)
				break;
			if (sublevel - level <= levels) {
				a.add(index);
			}
		}
		int[] aa = new int[a.size()];
		for (int i = 0; i < a.size(); i++) {
			aa[i] = a.get(i);
		}
		return aa;
	}

	/**
	 * Get the IPTC code at the specified level
	 * 
	 * @param iptc
	 * @return
	 */
	private int getLevelIptc(int iptc, int level) {
		int pow = (int) Math.pow(1000, 2 - level);
		return ((int) (iptc / pow)) * pow;
	}

	/**
	 * returns the level of the IPTC code in the hierarchy, starting with 1.
	 * returns 0 for "unknown" or "ambiguous".
	 * 
	 * @param iptc
	 * @return
	 */
	private int getLevel(int iptc) {
		if (iptc % 1000000 == 0) {
			return 1;
		}
		if (iptc % 1000 == 0) {
			return 2;
		}
		if (iptc > 2) {
			return 3;
		}
		return 0;
	}

	private static final Object iptcCodes[][] = {
			{ 0000001, "unknown" },
			{ 0000002, "ambiguous" }, // ///////////////
			{ 1000000, "culture" }, // ///////////////
			{ 1001000, "archaeology" },
			{ 1002000, "architecture" },
			{ 1003000, "bullfighting" },
			{ 1004000, "festive event (including carnival)" },
			{ 1005000, "cinema" },
			{ 1006000, "dance" },
			{ 1006001, "Dpa �ffentlicher Dienst" },
			{ 1007000, "fashion" },
			{ 1008000, "language" },
			{ 1009000, "library and museum" },
			{ 1010000, "literature" },
			{ 1011000, "music" },
			{ 1012000, "painting" },
			{ 1013000, "photography" },
			{ 1014000, "radio" },
			{ 1015000, "sculpture" },
			{ 1016000, "television" },
			{ 1017000, "theatre" },
			{ 1018000, "monument and heritage site" },
			{ 1019000, "customs and tradition" },
			{ 1020000, "arts (general)" },
			{ 1021000, "entertainment (general)" },
			{ 1022000, "culture (general)" },
			{ 1023000, "nightclub" },
			{ 1024000, "cartoon" },
			{ 1025000, "animation" },
			{ 2000000, "justice" }, // ///////////////
			{ 2001000, "crime" },
			{ 2002000, "judiciary (system of justice)" },
			{ 2003000, "police" },
			{ 2004000, "punishment" },
			{ 2005000, "prison" },
			{ 2006000, "laws" },
			{ 2007000, "justice and rights" },
			{ 2008000, "trials" },
			{ 2009000, "prosecution" },
			{ 2010000, "organized crime" },
			{ 2011000, "international law" },
			{ 2012000, "corporate crime" },
			{ 2012001, "fraud" },
			{ 2012002, "embezzlement" },
			{ 2012003, "restraint of trade" },
			{ 2012004, "breach of contract" },
			{ 2012005, "anti-trust crime" },
			{ 3000000, "disaster" }, // ///////////////
			{ 3001000, "drought" },
			{ 3002000, "earthquake" },
			{ 3003000, "famine" },
			{ 3004000, "fire" },
			{ 3005000, "flood" },
			{ 3006000, "industrial accident" },
			{ 3007000, "meteorological disaster" },
			{ 3008000, "nuclear accident" },
			{ 3009000, "pollution" },
			{ 3010000, "transport accident" },
			{ 3010001, "road accident" },
			{ 3010002, "railway accident" },
			{ 3010003, "air and space accident" },
			{ 3010004, "maritime accident" },
			{ 3011000, "volcanic eruption" },
			{ 3012000, "relief and aid organisation" },
			{ 3013000, "accident (general)" },
			{ 3014000, "emergency incident" },
			{ 3015000, "disaster (general)" },
			{ 3016000, "emergency planning" },
			{ 4000000, "economy" }, // ///////////////
			{ 4001000, "agriculture" },
			{ 4001001, "arable farming" },
			{ 4001002, "fishing industry" },
			{ 4001003, "forestry and timber" },
			{ 4001004, "livestock farming" },
			{ 4001005, "viniculture" },
			{ 4002000, "chemicals" },
			{ 4002001, "biotechnology" },
			{ 4002002, "fertiliser" },
			{ 4002003, "health and beauty product" },
			{ 4002004, "inorganic chemical" },
			{ 4002005, "organic chemical" },
			{ 4002006, "pharmaceutical" },
			{ 4002007, "synthetic and plastic" },
			{ 4003000, "computing and information" },
			{ 4003001, "hardware" },
			{ 4003002, "networking" },
			{ 4003003, "satellite technology" },
			{ 4003004, "semiconductors and active" },
			{ 4003005, "software" },
			{ 4003006, "telecommunication equipment" },
			{ 4003007, "telecommunication service" },
			{ 4003008, "security" },
			{ 4004000, "construction and property" },
			{ 4004001, "heavy construction" },
			{ 4004002, "house building" },
			{ 4004003, "real estate" },
			{ 4005000, "energy and resource" },
			{ 4005001, "alternative energy" },
			{ 4005002, "coal" },
			{ 4005003, "oil and gas - downstream" },
			{ 4005004, "oil and gas - upstream" },
			{ 4005005, "nuclear power" },
			{ 4005006, "electricity production and" },
			{ 4005007, "waste management and" },
			{ 4005008, "water supply" },
			{ 4005009, "natural resources" },
			{ 4005010, "energy (general)" },
			{ 4006000, "financial and business service" },
			{ 4006001, "accountancy and auditing" },
			{ 4006002, "banking" },
			{ 4006003, "consultancy service" },
			{ 4006004, "employment agency" },
			{ 4006005, "healthcare provider" },
			{ 4006006, "insurance" },
			{ 4006007, "legal service" },
			{ 4006008, "market research" },
			{ 4006009, "stock broking" },
			{ 4006010, "personal investing" },
			{ 4006011, "market trend" },
			{ 4006012, "shipping service" },
			{ 4006013, "personal service" },
			{ 4006014, "janitorial service" },
			{ 4006015, "funeral parlour and" },
			{ 4006016, "rental service" },
			{ 4006017, "wedding service" },
			{ 4007000, "consumer goods" },
			{ 4007001, "clothing" },
			{ 4007002, "department store" },
			{ 4007003, "food" },
			{ 4007004, "mail order" },
			{ 4007005, "retail" },
			{ 4007006, "speciality store" },
			{ 4007007, "wholesale" },
			{ 4007008, "beverage" },
			{ 4007009, "electronic commerce" },
			{ 4007010, "luxury good" },
			{ 4007011, "non-durable good" },
			{ 4008000, "macro economics" },
			{ 4008001, "central bank" },
			{ 4008002, "consumer issue" },
			{ 4008003, "debt market" },
			{ 4008004, "economic indicator" },
			{ 4008005, "emerging market" },
			{ 4008006, "foreign exchange market" },
			{ 4008007, "government aid" },
			{ 4008008, "government debt" },
			{ 4008009, "interest rate" },
			{ 4008010, "international economic" },
			{ 4008011, "international (foreign) trade" },
			{ 4008012, "loan market" },
			{ 4008013, "economic organization" },
			{ 4008014, "consumer confidence" },
			{ 4008015, "trade dispute" },
			{ 4009000, "market and exchange" },
			{ 4009001, "energy" },
			{ 4009002, "metal" },
			{ 4009003, "securities" },
			{ 4009004, "soft commodity" },
			{ 4010000, "media" },
			{ 4010001, "advertising" },
			{ 4010002, "book" },
			{ 4010003, "cinema industry" },
			{ 4010004, "news agency" },
			{ 4010005, "newspaper and magazine" },
			{ 4010006, "online" },
			{ 4010007, "public relation" },
			{ 4010008, "radio industry" },
			{ 4010009, "satellite and cable service" },
			{ 4010010, "television industry" },
			{ 4011000, "metal goods and engineering" },
			{ 4011001, "aerospace" },
			{ 4011002, "automotive equipment" },
			{ 4011003, "defence equipment" },
			{ 4011004, "electrical appliance" },
			{ 4011005, "heavy engineering" },
			{ 4011006, "industrial component" },
			{ 4011007, "instrument engineering" },
			{ 4011008, "shipbuilding" },
			{ 4011009, "machine manufacturing" },
			{ 4011010, "Dpa Fahrzeugbau" },
			{ 4012000, "metal and mineral" },
			{ 4012001, "building material" },
			{ 4012002, "gold and precious material" },
			{ 4012003, "iron and steel" },
			{ 4012004, "non ferrous metal" },
			{ 4012005, "mining" },
			{ 4013000, "process industry" },
			{ 4013001, "distiller and brewer" },
			{ 4013002, "food" },
			{ 4013003, "furnishings and furniture" },
			{ 4013004, "paper and packaging product" },
			{ 4013005, "rubber product" },
			{ 4013006, "soft drinks" },
			{ 4013007, "textile and clothing" },
			{ 4013008, "tobacco" },
			{ 4013009, "Dpa Holz" },
			{ 4013010, "Dpa Spielwaren" },
			{ 4014000, "tourism and leisure" },
			{ 4014001, "casino and gambling" },
			{ 4014002, "hotel and accommodation" },
			{ 4014003, "recreational and sporting" },
			{ 4014004, "restaurant and catering" },
			{ 4014005, "tour operator" },
			{ 4015000, "transport" },
			{ 4015001, "air transport" },
			{ 4015002, "railway" },
			{ 4015003, "road transport" },
			{ 4015004, "waterway and maritime" },
			{ 4015005, "Dpa Postdienste" },
			{ 4016000, "company information" },
			{ 4016001, "accounting and audit" },
			{ 4016002, "annual and special" },
			{ 4016003, "annual report" },
			{ 4016004, "antitrust issue" },
			{ 4016005, "merger, acquisition and" },
			{ 4016006, "analysts' comment" },
			{ 4016007, "bankruptcy" },
			{ 4016008, "board of directors" },
			{ 4016009, "buyback" },
			{ 4016010, "C.E.O. interview" },
			{ 4016011, "corporate officer" },
			{ 4016012, "corporate profile" },
			{ 4016013, "contract" },
			{ 4016014, "defence contract" },
			{ 4016015, "dividend announcement" },
			{ 4016016, "earnings forecast" },
			{ 4016017, "financially distressed" },
			{ 4016018, "earnings" },
			{ 4016019, "financing and stock" },
			{ 4016020, "government contract" },
			{ 4016021, "global expansion" },
			{ 4016022, "insider trading" },
			{ 4016023, "joint venture" },
			{ 4016024, "leveraged buyout" },
			{ 4016025, "layoffs and downsizing" },
			{ 4016026, "licensing agreement" },
			{ 4016027, "litigation and regulation" },
			{ 4016028, "management change" },
			{ 4016029, "marketing" },
			{ 4016030, "new product" },
			{ 4016031, "patent, copyright and" },
			{ 4016032, "plant closing" },
			{ 4016033, "plant opening" },
			{ 4016034, "privatisation" },
			{ 4016035, "proxy filing" },
			{ 4016036, "rating" },
			{ 4016037, "research and development" },
			{ 4016038, "quarterly or semiannual" },
			{ 4016039, "restructuring and" },
			{ 4016040, "spin-off" },
			{ 4016041, "stock activity" },
			{ 4017000, "economy (general)" },
			{ 4018000, "business (general)" },
			{ 4019000, "finance (general)" },
			{ 5000000, "education" }, // /////////////////
			{ 5001000, "adult education" },
			{ 5002000, "further education" },
			{ 5003000, "parent organisation" },
			{ 5004000, "preschool" },
			{ 5005000, "school" },
			{ 5006000, "teachers union" },
			{ 5007000, "university" },
			{ 5008000, "upbringing" },
			{ 5009000, "entrance examination" },
			{ 6000000, "environmental_issue" }, // ///////
			{ 6001000, "renewable energy" },
			{ 6002000, "conservation" },
			{ 6003000, "energy saving" },
			{ 6004000, "environmental politics" },
			{ 6005000, "environmental pollution" },
			{ 6006000, "natural resources" },
			{ 6007000, "nature" },
			{ 6008000, "population" },
			{ 6009000, "waste" },
			{ 6010000, "water" },
			{ 6011000, "global warming" },
			{ 7000000, "health" }, // ////////////////
			{ 7001000, "disease" },
			{ 7002000, "epidemic and plague" },
			{ 7003000, "health treatment" },
			{ 7004000, "health organisations" },
			{ 7005000, "medical research" },
			{ 7006000, "medical staff" },
			{ 7006001, "Dpa �rzte" },
			{ 7006002, "Dpa Pflegeberufe" },
			{ 7007000, "medicine" },
			{ 7008000, "preventative medicine" },
			{ 7009000, "injury" },
			{ 7010000, "hospital and clinic" },
			{ 7011000, "government health care" },
			{ 7012000, "private health care" },
			{ 7013000, "healthcare policy" },
			{ 7014000, "medical specialisation" },
			{ 7014001, "geriatric" },
			{ 7014002, "pediatrics" },
			{ 7014003, "reproduction" },
			{ 7014004, "genetics" },
			{ 7015000, "medical service" },
			{ 8000000, "human_interest" }, // /////////////
			{ 8001000, "animal" },
			{ 8002000, "curiosity" },
			{ 8003000, "people" },
			{ 8003001, "advice" },
			{ 8003002, "celebrity" },
			{ 8004000, "mystery" },
			{ 8005000, "society" },
			{ 8005001, "DPA Szene" },
			{ 8006000, "award and prize" },
			{ 8007000, "imperial and royal matters" },
			{ 9000000, "labour" }, // /////////////
			{ 9001000, "apprentices" },
			{ 9002000, "collective contract" },
			{ 9003000, "employment" },
			{ 9004000, "labour dispute" },
			{ 9005000, "labour legislation" },
			{ 9006000, "retirement" },
			{ 9007000, "retraining" },
			{ 9008000, "strike" },
			{ 9009000, "unemployment" },
			{ 9010000, "unions" },
			{ 9011000, "wage and pension" },
			{ 9012000, "work relations" },
			{ 9013000, "health and safety at work" },
			{ 9014000, "advanced training" },
			{ 9015000, "employer" },
			{ 9016000, "employee" },
			{ 10000000, "leisure" }, // //////////////
			{ 10001000, "game" },
			{ 10001001, "Go" },
			{ 10001002, "chess" },
			{ 10001003, "bridge" },
			{ 10002000, "gaming and lottery" },
			{ 10003000, "gastronomy" },
			{ 10004000, "hobby" },
			{ 10005000, "holiday or vacation" },
			{ 10006000, "tourism" },
			{ 10007000, "travel and commuting" },
			{ 10008000, "club and association" },
			{ 10009000, "lifestyle (house and home)" },
			{ 10010000, "leisure (general)" },
			{ 10011000, "public holiday" },
			{ 10012000, "hunting" },
			{ 10013000, "fishing" },
			{ 11000000, "politics" }, // //////////////
			{ 11001000, "defence" },
			{ 11001001, "veterans affairs" },
			{ 11002000, "diplomacy" },
			{ 11002001, "summit" },
			{ 11003000, "election" },
			{ 11004000, "espionage and intelligence" },
			{ 11005000, "foreign aid" },
			{ 11006000, "government" },
			{ 11006001, "civil and public service" },
			{ 11006002, "safety of citizens" },
			{ 11006003, "think tank" },
			{ 11006004, "Dpa Koalition" },
			{ 11007000, "human rights" },
			{ 11008000, "local authority" },
			{ 11009000, "parliament" },
			{ 11009002, "Dpa Bundesrat" },
			{ 11009003, "Dpa Bundestag" },
			{ 11009004, "Dpa B�rgerschaft" },
			{ 11009006, "Dpa Europarat" },
			{ 11009007, "Dpa Landtag" },
			{ 11009008, "Dpa Senat" },
			{ 11010000, "parties and movements" },
			{ 11011000, "refugee" },
			{ 11012000, "regional authority" },
			{ 11013000, "state budget and tax" },
			{ 11013001, "Dpa Haushalt" },
			{ 11013002, "Dpa Staatsoberhaupt" },
			{ 11013003, "Dpa Hauptstadt" },
			{ 11014000, "treaty and international" },
			{ 11014001, "Dpa EU" },
			{ 11014004, "Dpa WEU" },
			{ 11014005, "Dpa G8" },
			{ 11014006, "Dpa G7" },
			{ 11014007, "Dpa UN" },
			{ 11014008, "Dpa GUS" },
			{ 11015000, "constitution" },
			{ 11016000, "interior policy" },
			{ 11016001, "data protection" },
			{ 11016002, "housing and urban planning" },
			{ 11016003, "pension and welfare" },
			{ 11016004, "personal weapon control" },
			{ 11017000, "migration" },
			{ 11017001, "Dpa Bundespr�sident" },
			{ 11018000, "citizens initiative and recall" },
			{ 11019000, "referenda" },
			{ 11020000, "nuclear policy" },
			{ 11021000, "lobbying" },
			{ 11022000, "regulatory policy and" },
			{ 11023000, "censorship" },
			{ 12000000, "religion" }, // /////////////
			{ 12001000, "cult and sect" },
			{ 11025000, "Dpa Steuern" },
			{ 11028000, "Dpa Aff�ren" },
			{ 11029000, "Dpa Extremismus" },
			{ 12002000, "belief (faith)" },
			{ 12003000, "freemasonry" },
			{ 12004000, "religion" },
			{ 12005000, "church (organisation)" },
			{ 13000000, "science" }, // ///////////
			{ 13001000, "applied science" },
			{ 13002000, "engineering" },
			{ 13003000, "human science" },
			{ 13004000, "natural science" },
			{ 13005000, "philosophical science" },
			{ 13006000, "research" },
			{ 13007000, "scientific exploration" },
			{ 13008000, "space programme" },
			{ 13009000, "science (general)" },
			{ 13010000, "technology (general)" },
			{ 13011000, "standards" },
			{ 13012000, "animal science" },
			{ 13013000, "micro science" },
			{ 13014000, "marine science" },
			{ 13015000, "weather science" },
			{ 13016000, "electronics" },
			{ 13017000, "identification technology" },
			{ 14000000, "social_issue" }, // //////////////
			{ 14001000, "addiction" },
			{ 14002000, "charity" },
			{ 14003000, "demographics" },
			{ 14004000, "disabled" },
			{ 14005000, "euthanasia (also includes" },
			{ 14006000, "family" },
			{ 14007000, "family planning" },
			{ 14008000, "health insurance" },
			{ 14009000, "homelessness" },
			{ 14010000, "minority group" },
			{ 14011000, "pornography" },
			{ 14012000, "poverty" },
			{ 14013000, "prostitution" },
			{ 14014000, "racism" },
			{ 14015000, "welfare" },
			{ 14016000, "abortion" },
			{ 14017000, "missing person" },
			{ 14018000, "long term care" },
			{ 14019000, "juvenile delinquency" },
			{ 14020000, "nuclear radiation victims" },
			{ 14021000, "slavery" },
			{ 14022000, "abusive behaviour" },
			{ 15000000, "sport" }, // ////////////
			{ 15000001, "Dpa Behindertensport" },
			{ 15000002, "Dpa Breitensport" },
			{ 15000004, "Dpa Leistungssport" }, { 15000005, "Dpa Sporthilfe" },
			{ 15000006, "Dpa Sportlerwahl" }, { 15000007, "Dpa Sportmedizin" },
			{ 15000008, "Dpa Sportpolitik" },
			{ 15001000, "aero and aviation sport" },
			{ 15001001, "parachuting" },
			{ 15001002, "sky diving Dpa Segelfliegen" },
			{ 15002000, "alpine skiing" }, { 15002001, "downhill" },
			{ 15002002, "giant slalom" }, { 15002003, "super G" },
			{ 15002004, "slalom" }, { 15002005, "combined" },
			{ 15003000, "American football" },
			{ 15003001, "(US) National Football" },
			{ 15003002, "entry deleted" }, { 15003003, "AFL" },
			{ 15004000, "archery" }, { 15004001, "FITA" },
			{ 15005000, "athletics, track and field" }, { 15005001, "100 m" },
			{ 15005002, "200 m" }, { 15005003, "400 m" },
			{ 15005004, "800 m" }, { 15005005, "1000 m" },
			{ 15005006, "1500 m" }, { 15005007, "mile" },
			{ 15005008, "2000 m" }, { 15005009, "3000 m" },
			{ 15005010, "5000 m" }, { 15005011, "10,000 m" },
			{ 15005012, "20 km" }, { 15005013, "one hour" },
			{ 15005014, "25000" }, { 15005015, "30000" },
			{ 15005016, "110 m hurdles" }, { 15005017, "400 m hurdles" },
			{ 15005018, "3000 m steeplechase" }, { 15005019, "high jump" },
			{ 15005020, "pole vault" }, { 15005021, "long jump" },
			{ 15005022, "triple jump" }, { 15005023, "shot put" },
			{ 15005024, "discus throw" }, { 15005025, "hammer throw" },
			{ 15005026, "javelin throw" }, { 15005027, "decathlon" },
			{ 15005028, "4x100 m" }, { 15005029, "4x200 m" },
			{ 15005030, "4x400 m" }, { 15005031, "4x800 m" },
			{ 15005032, "4x1500 m" }, { 15005033, "walk 1 h" },
			{ 15005034, "walk 2 h" }, { 15005035, "10 km walk" },
			{ 15005036, "15 km walk" }, { 15005037, "20 km walk" },
			{ 15005038, "30 km walk" }, { 15005039, "50 km walk" },
			{ 15005040, "100 m hurdles" }, { 15005041, "5 km walk" },
			{ 15005042, "heptathlon" }, { 15005043, "1500 m walk" },
			{ 15005044, "2000 m walk" }, { 15005045, "3000 m walk" },
			{ 15005046, "50 m" }, { 15005047, "50 m hurdles" },
			{ 15005048, "50 yards" }, { 15005049, "50 yard hurdles" },
			{ 15005050, "60 m" }, { 15005051, "60 m hurdles" },
			{ 15005052, "60 yards" }, { 15005053, "60 yard hurdles" },
			{ 15005054, "100 yards" }, { 15005055, "100 yard hurdles" },
			{ 15005056, "300 m" }, { 15005057, "300 yards" },
			{ 15005058, "440 yards" }, { 15005059, "500 m" },
			{ 15005060, "500 yards" }, { 15005061, "600 m" },
			{ 15005062, "600 yards" }, { 15005063, "880 yards" },
			{ 15005064, "1000 yards" }, { 15005065, "2 miles" },
			{ 15005066, "3 miles" }, { 15005067, "6 miles" },
			{ 15005068, "4x1 mile" }, { 15006000, "badminton" },
			{ 15007000, "baseball" }, { 15007001, "Major League Baseball" },
			{ 15007002, "Major League Baseball" },
			{ 15007003, "Major League Baseball" },
			{ 15007004, "rubberball baseball" }, { 15008000, "basketball" },
			{ 15008001, "National Basketball" },
			{ 15008002, "professional - Women general" },
			{ 15009000, "biathlon" }, { 15009001, "7.5 km" },
			{ 15009002, "10 km" }, { 15009003, "15 km" },
			{ 15009004, "20 km" }, { 15009005, "4x7.5 km relay" },
			{ 15009006, "12.5 km pursuit" },
			{ 15010000, "billiards, snooker and pool" },
			{ 15010001, "8 ball" }, { 15010002, "9 ball" },
			{ 15010003, "14.1" }, { 15010004, "continuous" },
			{ 15010005, "other" }, { 15011000, "bobsleigh" },
			{ 15011001, "two-man sled" }, { 15011002, "four-man sled" },
			{ 15012000, "bowling" }, { 15013000, "bowls and petanque" },
			{ 15014000, "boxing" }, { 15014001, "super-heavyweight" },
			{ 15014002, "heavyweight" }, { 15014003, "cruiserweight" },
			{ 15014004, "light-heavyweight" },
			{ 15014005, "super-middleweight" }, { 15014006, "middleweight" },
			{ 15014007, "light-middleweight" }, { 15014008, "welterweight" },
			{ 15014009, "light-welterweight" }, { 15014010, "lightweight" },
			{ 15014011, "super-featherweight" }, { 15014012, "featherweight" },
			{ 15014013, "super-bantamweight" }, { 15014014, "bantamweight" },
			{ 15014015, "super-flyweight" }, { 15014016, "flyweight" },
			{ 15014017, "light flyweight" }, { 15014018, "straw" },
			{ 15014019, "IBF" }, { 15014020, "WBA" }, { 15014021, "WBC" },
			{ 15014022, "WBO" }, { 15014023, "French boxing" },
			{ 15014024, "Thai boxing" }, { 15015000, "canoeing and kayaking" },
			{ 15015001, "Slalom" }, { 15015002, "200 m" },
			{ 15015003, "500 m" }, { 15015004, "1000 m" }, { 15015005, "K1" },
			{ 15015006, "K2" }, { 15015007, "K4" }, { 15015008, "C1" },
			{ 15015009, "C2" }, { 15015010, "C4" },
			{ 15015011, "canoe sailing" }, { 15016000, "climbing" },
			{ 15017000, "cricket" }, { 15018000, "curling" },
			{ 15019000, "cycling" }, { 15019001, "track" },
			{ 15019002, "pursuit" }, { 15019003, "Olympic sprint" },
			{ 15019004, "sprint" }, { 15019005, "Keirin" },
			{ 15019006, "points race" }, { 15019007, "Madison race" },
			{ 15019008, "500 m time trial" }, { 15019009, "1 km time trial" },
			{ 15019010, "one hour" }, { 15019011, "road race" },
			{ 15019012, "road time trial" }, { 15019013, "staging race" },
			{ 15019014, "cyclo-cross" }, { 15019015, "Vtt" },
			{ 15019016, "Vtt-cross" }, { 15019017, "Vtt-downhill" },
			{ 15019018, "bi-crossing" }, { 15019019, "trial" },
			{ 15020000, "dancing" }, { 15021000, "diving" },
			{ 15021001, "10 m platform" },
			{ 15021002, "10 m platform synchronised" },
			{ 15021003, "3 m springboard" },
			{ 15021004, "3 m springboard synchronised" },
			{ 15021005, "subaquatics" }, { 15022000, "equestrian" },
			{ 15022001, "three-day event" }, { 15022002, "dressage" },
			{ 15022003, "jumping" }, { 15022004, "cross country" },
			{ 15023000, "fencing" }, { 15023001, "epee" },
			{ 15023002, "foil" }, { 15023003, "sabre" },
			{ 15024000, "field Hockey" }, { 15025000, "figure Skating" },
			{ 15025001, "singles" }, { 15025002, "pairs" },
			{ 15025003, "ice dance" }, { 15026000, "freestyle Skiing" },
			{ 15026001, "moguls" }, { 15026002, "aerials" },
			{ 15026003, "artistic skiing" }, { 15027000, "golf" },
			{ 15028000, "gymnastics" }, { 15028001, "floor exercise" },
			{ 15028002, "vault" }, { 15028003, "pommel horse" },
			{ 15028004, "uneven bars" }, { 15028005, "parallel bars" },
			{ 15028006, "horizontal bar" }, { 15028007, "rings" },
			{ 15028008, "beam" }, { 15028009, "rythmic" },
			{ 15028010, "clubs" }, { 15028011, "hoop" },
			{ 15028012, "ribbon" }, { 15028013, "rope" }, { 15028014, "ball" },
			{ 15028015, "trampoline" }, { 15029000, "handball (team)" },
			{ 15030000, "horse racing, harness racing" },
			{ 15030001, "flat racing" }, { 15030002, "steeple chase" },
			{ 15030003, "trotting" }, { 15030004, "cross country" },
			{ 15031000, "ice hockey" }, { 15031001, "National Hockey League" },
			{ 15031002, "sledge hockey" }, { 15032000, "Jai Alai (Pelota)" },
			{ 15032001, "fronton" }, { 15032002, "jai-alai" },
			{ 15032003, "left wall" }, { 15032004, "trinquet" },
			{ 15032005, "rebot" }, { 15032006, "chistera anch." },
			{ 15032007, "chistera corta." }, { 15032008, "bare hand." },
			{ 15032009, "pala-ancha" }, { 15032010, "pala-corta" },
			{ 15032011, "pasaka" }, { 15032012, "xare" }, { 15033000, "judo" },
			{ 15033001, "heavyweight" }, { 15033002, "half-heavyweight" },
			{ 15033003, "middleweight" }, { 15033004, "half-middleweight" },
			{ 15033005, "half-lightweight" }, { 15033006, "lightweight" },
			{ 15033007, "extra lightweight" }, { 15034000, "karate" },
			{ 15034001, "sparring" }, { 15034002, "formal exercise" },
			{ 15035000, "lacrosse" }, { 15036000, "luge" },
			{ 15036001, "singles" }, { 15036002, "doubles" },
			{ 15037000, "marathon" }, { 15038000, "modern pentathlon" },
			{ 15038001, "running" }, { 15038002, "shooting" },
			{ 15038003, "swimming" }, { 15038004, "fencing" },
			{ 15038005, "showjumping" }, { 15039000, "motor racing" },
			{ 15039001, "Formula One" }, { 15039002, "F3000" },
			{ 15039003, "endurance" }, { 15039004, "Indy" },
			{ 15039005, "CART" }, { 15039006, "NHRA" }, { 15039007, "NASCAR" },
			{ 15039008, "TRUCKI" }, { 15040000, "motor rallying" },
			{ 15040001, "rallying" }, { 15040002, "raid" },
			{ 15040003, "rallycross" }, { 15041000, "motorcycling" },
			{ 15041001, "speed-Grand-Prix" }, { 15041002, "enduro" },
			{ 15041003, "grass-track" }, { 15041004, "moto-ball" },
			{ 15041005, "moto-cross" }, { 15041006, "rallying" },
			{ 15041007, "trial" }, { 15041008, "endurance" },
			{ 15041009, "superbike" }, { 15041010, "125 cm3" },
			{ 15041011, "250 cm3" }, { 15041012, "500 cm3" },
			{ 15041013, "side-cars" }, { 15042000, "netball" },
			{ 15043000, "nordic skiing" }, { 15043001, "cross-country" },
			{ 15043002, "5 km classical time" },
			{ 15043003, "10 km classical style" },
			{ 15043004, "10 km pursuit free style" },
			{ 15043005, "15 km classical style" },
			{ 15043006, "15 km pursuit free style" },
			{ 15043007, "10 km + 15 km combined" },
			{ 15043008, "30 km classical style" },
			{ 15043009, "30km free style" }, { 15043010, "50 km free style" },
			{ 15043011, "4x5 km relay" }, { 15043012, "4x10 km relay" },
			{ 15043013, "nordic combined" }, { 15043014, "raid" },
			{ 15043015, "5 km pursuit free style" },
			{ 15043016, "1.5 km sprint free" },
			{ 15043017, "50 km classic style" }, { 15044000, "orienteering" },
			{ 15044001, "Ski orienteering" }, { 15045000, "polo" },
			{ 15046000, "power boating" }, { 15046001, "F1" },
			{ 15046002, "F2" }, { 15047000, "rowing" },
			{ 15047001, "single sculls" }, { 15047002, "double sculls" },
			{ 15047003, "quadruple sculls" }, { 15047004, "coxless pair" },
			{ 15047005, "coxless four" }, { 15047006, "eight" },
			{ 15047007, "lightweight" }, { 15048000, "rugby league" },
			{ 15049000, "rugby union" }, { 15049001, "rugby 7" },
			{ 15050000, "sailing" }, { 15050001, "Tornado" },
			{ 15050002, "soling" }, { 15050003, "49er" },
			{ 15050004, "Europe" }, { 15050005, "Laser" }, { 15050006, "470" },
			{ 15050007, "Finn" }, { 15050008, "Star" },
			{ 15050009, "flying dutchmann" }, { 15050010, "505" },
			{ 15050011, "staging race" }, { 15050012, "around the world" },
			{ 15050013, "monohull" }, { 15050014, "multihulls" },
			{ 15051000, "shooting" }, { 15051001, "10 m air rifle" },
			{ 15051002, "10 m air pistol" },
			{ 15051003, "10 m running target" },
			{ 15051004, "25 m rapid fire pistol" },
			{ 15051005, "25 m sport pistol" },
			{ 15051006, "50 m free pistol" },
			{ 15051007, "50 m free rifle prone" },
			{ 15051008, "50 m free rifle 3x40" },
			{ 15051009, "50 m sport rifle 3x20" }, { 15051010, "trap" },
			{ 15051011, "double trap" }, { 15051012, "skeet" },
			{ 15052000, "ski jumping" }, { 15052001, "K90 jump" },
			{ 15052002, "K120 jump" }, { 15052003, "K180 (flying jump)" },
			{ 15053000, "snow boarding" }, { 15053001, "giant slalom" },
			{ 15053002, "half-pipe" }, { 15054000, "soccer" },
			{ 15055000, "softball" }, { 15056000, "speed skating" },
			{ 15056001, "500 m" }, { 15056002, "1000 m" },
			{ 15056003, "1500 m" }, { 15056004, "3000 m" },
			{ 15056005, "5000 m" }, { 15056006, "10000 m" },
			{ 15056007, "Short-track" }, { 15056008, "st 500 m" },
			{ 15056009, "st 1000m" }, { 15056010, "st 1500m" },
			{ 15056011, "st 3000m" }, { 15056012, "st 3000m relay" },
			{ 15056013, "st 5000m" }, { 15056014, "st 5000m relay" },
			{ 15057000, "speedway" }, { 15058000, "sports organisations" },
			{ 15058001, "IOC" }, { 15058002, "international federation" },
			{ 15058003, "continental federation" },
			{ 15058004, "national federation" }, { 15058005, "GAISF" },
			{ 15059000, "squash" }, { 15060000, "sumo wrestling" },
			{ 15061000, "surfing" }, { 15062000, "swimming" },
			{ 15062001, "50 m freestyle" }, { 15062002, "100 m freestyle" },
			{ 15062003, "200 m freestyle" }, { 15062004, "400 m freestyle" },
			{ 15062005, "800 m freestyle" }, { 15062006, "1500 m freestyle" },
			{ 15062007, "relay 4x50 m freestyle" },
			{ 15062008, "relay 4x100 m freestyle" },
			{ 15062009, "relay 4x200 m freestyle" },
			{ 15062010, "50 m backstroke" }, { 15062011, "100 m backstroke" },
			{ 15062012, "200 m backstroke" },
			{ 15062013, "50 m breaststroke" },
			{ 15062014, "100 m breaststroke" },
			{ 15062015, "200 m breaststroke" }, { 15062016, "50 m butterfly" },
			{ 15062017, "100 m butterfly" }, { 15062018, "200 m butterfly" },
			{ 15062019, "100 m medley" }, { 15062020, "200 m medley" },
			{ 15062021, "400 m medley" }, { 15062022, "relay 4x50 m medlay" },
			{ 15062023, "relay4x100 m medley" }, { 15062024, "short course" },
			{ 15062025, "synchronised technical" },
			{ 15062026, "synchronised free routine" },
			{ 15063000, "table tennis" }, { 15064000, "Taekwon-Do" },
			{ 15064001, "under 49 kg" }, { 15064002, "under 58 kg" },
			{ 15064003, "49-57 kg" }, { 15064004, "58-68 kg" },
			{ 15064005, "57-67 kg" }, { 15064006, "68-80 kg" },
			{ 15064007, "over 67 kg" }, { 15064008, "over 80 kg" },
			{ 15065000, "tennis" }, { 15066000, "triathlon" },
			{ 15066001, "triathlon swimming" },
			{ 15066002, "triathlon cycling" }, { 15066003, "triathlon run" },
			{ 15067000, "volleyball" }, { 15067001, "beach volleyball" },
			{ 15068000, "water polo" }, { 15069000, "water skiing" },
			{ 15069001, "slalom" }, { 15069002, "trick" },
			{ 15069003, "jump" }, { 15069004, "combined" },
			{ 15070000, "weightlifting" }, { 15070001, "snatch" },
			{ 15070002, "clean and jerk" }, { 15070003, "48 kg" },
			{ 15070004, "53 kg" }, { 15070005, "63 kg" },
			{ 15070006, "75 kg" }, { 15070007, "over 75 kg" },
			{ 15070008, "56 kg" }, { 15070009, "62 kg" },
			{ 15070010, "69 kg" }, { 15070011, "77 kg" },
			{ 15070012, "85 kg" }, { 15070013, "94 kg" },
			{ 15070014, "105 kg" }, { 15070015, "over 105 kg" },
			{ 15070016, "powerlifting" }, { 15071000, "windsurfing" },
			{ 15071001, "ocean" }, { 15071002, "lake" }, { 15071003, "river" },
			{ 15071004, "land" }, { 15072000, "wrestling" },
			{ 15072001, "freestyle" }, { 15072002, "greco-roman" },
			{ 15072003, "over 130 kg" }, { 15072004, "130 kg" },
			{ 15072005, "97 kg" }, { 15072006, "85 kg" },
			{ 15072007, "76 kg" }, { 15072008, "69 kg" },
			{ 15072009, "63 kg" }, { 15072010, "58 kg" },
			{ 15072011, "54 kg" }, { 15073000, "sports event" },
			{ 15073001, "Summer Olympics" }, { 15073002, "Winter Olympics" },
			{ 15073003, "Summer universiade" },
			{ 15073004, "Winter Universiade" },
			{ 15073005, "Commonwealth Games" },
			{ 15073006, "Winter Goodwill Games" },
			{ 15073007, "Summer Asian Games" },
			{ 15073008, "Winter Asian Games" },
			{ 15073009, "Panamerican Games" }, { 15073010, "African Games" },
			{ 15073011, "Mediterranean Games" },
			{ 15073012, "SouthEast Asiatic Games" },
			{ 15073013, "PanPacific Games" },
			{ 15073014, "SouthPacific Games" },
			{ 15073015, "PanArabic Games" },
			{ 15073016, "Summer Goodwill Games" }, { 15073017, "World games" },
			{ 15073018, "World Cup" }, { 15073019, "intercontinental cup" },
			{ 15073020, "continental cup" }, { 15073021, "international cup" },
			{ 15073022, "National Cup" }, { 15073023, "interregional cup" },
			{ 15073024, "regional cup" }, { 15073025, "league cup" },
			{ 15073026, "world championship" },
			{ 15073027, "intercontinental championship" },
			{ 15073028, "continental championship" },
			{ 15073029, "continental championship" },
			{ 15073030, "continental championship" },
			{ 15073031, "national championship 1st" },
			{ 15073032, "national championship 2nd" },
			{ 15073033, "national championship3rdlevel" },
			{ 15073034, "national championship 4th" },
			{ 15073035, "regional championship" }, { 15073036, "Grand Prix" },
			{ 15073037, "intercontinental tournament" },
			{ 15073038, "continental tournament" },
			{ 15073039, "international tournament" },
			{ 15073040, "national tournament" },
			{ 15073041, "inter-nations competition" },
			{ 15073042, "inter-clubs competition" },
			{ 15073043, "friendly competition" },
			{ 15073044, "all-stars competition" }, { 15073045, "exhibition" },
			{ 15074000, "rodeo" }, { 15074001, "barrel racing" },
			{ 15074002, "calf roping" }, { 15074003, "bull riding" },
			{ 15074004, "bulldogging" }, { 15074005, "saddle bronc" },
			{ 15074006, "bareback" }, { 15074007, "goat roping" },
			{ 15075000, "mini golf sport" }, { 15076000, "bandy" },
			{ 15077000, "flying disc" }, { 15077001, "ultimate" },
			{ 15077002, "guts" }, { 15077003, "overall" },
			{ 15077004, "distance" },
			{ 15077005, "discathon" },
			{ 15077006, "DDC" },
			{ 15077007, "SCF" },
			{ 15077008, "freestyle" },
			{ 15077009, "accuracy" },
			{ 15077010, "disc golf" },
			{ 15078000, "floorball" },
			{ 15079000, "casting" },
			{ 15080000, "tug-of-war" },
			{ 15081000, "croquette" },
			{ 15082000, "dog racing" },
			{ 15082001, "sled" },
			{ 15082002, "oval track" },
			{ 15083000, "skeleton" },
			{ 15084000, "Australian rules football" },
			{ 15085000, "Canadian football" },
			{ 16000000, "conflicts" }, // /////////////////
			{ 16001000, "act of terror" }, { 16002000, "armed conflict" },
			{ 16003000, "civil unrest" }, { 16004000, "coup d'etat" },
			{ 16005000, "guerrilla activity" }, { 16006000, "massacre" },
			{ 16007000, "riots" }, { 16008000, "violent demonstration" },
			{ 16009000, "war" },
			{ 16010000, "conflict (general)" },
			{ 16011000, "crisis" },
			{ 16012000, "weaponry" },
			{ 17000000, "weather" }, // ////////////////////
			{ 17001000, "forecast" }, { 17002000, "global change" },
			{ 17003000, "report" }, { 17004000, "statistic" },
			{ 17005000, "warning" } };

}

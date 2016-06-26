@include ('includes.header')

<div class="main-content">
	<div class="block">
		<div class="heading">Welcome to The Sentinel's Playground!</div>
		<div class="content">
			The Sentinel's Playground is _NOW_ a free, automated server host for Zandronum.
			The Sentinel's Playground existed since 2010 however it was shut down due to lack of funds. The Sentinel's Playground used to host a 
			small set of deathmatch and ctf servers that were not popular or were underground in efforts to get them played. 
			But now we have decided to allow all of you to run almost anything you want on our hardware. Best of all it's FREE!
			This service is also using BestBot (the same one that BestEver uses, so the commands are the same :D)
		</div>
		<div class="heading">Background</div>
		<div class="content">
			The Sentinel's Playground was built around 2010. It became a cluser that had meaning. The cluster was named after a song known as "The Sentinel" By Judas Priest. 
			The server was also home to All fear the sentinel Deathmatch. Both of these had meaning in the songs chours. Those lyrics were:
			<ul>
				<li>SWORN TO AVENGE!</li>
				<li>CONDEMN TO HELL</li>
				<li>TEMPT NOT THE BLADE</li>
				<LI>ALL FEAR THE SENTINEL!!!!</li>
			</ul>
		</div>
		<div class="heading">Why use [TSPG]</div>
		<div class="content">
			Well, since we are similar to BE, the same sort of features apply. Here they are for your reference!
			<ul>
				<li>Starting a server takes a few seconds</li>
				<li>Upload your own WAD/PK3 files and play the game your way</li>
				<li>Even if you turn off your computer/client, your server will stay up</li>
				<li>RCON access as well as log access to your server, granting you complete control</li>
				<li>Gigabit network to ensure your servers don't lag</li>
			</ul>
			The only difference is location. Our server is located at New York, USA. You are currently on the node <strong>Painkiller </strong>
		</div>
		<div class="heading">Getting started</div>
		<div class="content">
			The servers are managed on our irc channel, where the bot will start your server for you via commands.
			The full guide with all options can be viewed <a href="/host" target="_blank">here</a>, a quick guide is provided below.
			<ol style="margin-top: 10px">
				<li>Get an IRC client, connect to Zandronum's IRC server: irc.zandronum.com:6667, and join the <strong>#tspg-painkiller</strong> channel. If you're unsure on how to connect, or which
				client to use, you can take a look at Zandronum's IRC guide <a href="http://wiki.zandronum.com/IRC#Software" target="_blank">here</a>.</li>
				<li>Type <span class="code">/msg nickserv register &lt;password&gt; &lt;email&gt;</span></li>
				<li>Check your email, and follow the instructions to finish registering with Zandronum IRC</li>
				<li>After you have validated your account, register with Painkiller by typing <span class="code">/msg Painkiller register &lt;password&gt;</span>
				<li>Type <span class="code">.host iwad="doom2.wad" gamemode="deathmatch" wad="udm3.wad" hostname="My first server"</span>
					in the #tspg-painkiller channels to start your server</li>
				<li>This will start a duel server named "My first server" with the wad udm3.wad. Please see
					<a href="/host" target="_blank">this</a> guide for all hosting options.</li>
			</ol>
			<strong>*</strong> Please note that you'll need to re-identify each time you connect to IRC. You need to type
				<span class="code">/msg nickserv identify &lt;username&gt; &lt;password&gt;</span> (using your Zandronum IRC account information) each time you connect.
		</div>
	</div>
</div>

@include ('includes.footer')

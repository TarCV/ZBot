@include ('includes.header')

<div class="main-content">
	<div class="block">
		<div class="heading">Starting a server</div>
		<div class="content">
			Here is some information on some hosting commands! Make sure you have made an account first..
			All of the options listen below are in a option="value" format. These can be added to the end of your server hosting command to include additional options.
			As a default, we'll be using <span class="code">.host iwad="doom2.wad" hostname="My server" gamemode="duel"</span> as a reference, as those three options
			(iwad and hostname) are required to start a server. When you're done making the host command, simply send it as a message to #tspg-painkiller and it	should start!
		</div>
		<div class="heading">Host Options</div>
		<div class="content">
			<table class="table table-bordered" style="background: url('/static/img/bg.png') repeat">
				<tr>
					<th>Option</th>
					<th>Description</th>
					<th>Example</th>
				</tr>
				<tr>
					<td><strong>Iwad</strong></td>
					<td>Adds an IWAD to your server. Each server can only have one IWAD.</td>
					<td><span class="code">iwad="doom2.wad"</span></td>
				</tr>
				<tr>
					<td><strong>Hostname</strong></td>
					<td>Sets the hostname for the server. This is what will appear on the master server list.</td>
					<td><span class="code">hostname="Test name"</span></td>
				</tr>
				<tr>
					<td><strong>Gamemode</strong></td>
					<td>Sets the gamemode for the server.</td>
					<td><span class="code">gamemode="coop"</span></td>
				</tr>
				<tr>
					<td><strong>Wad</strong></td>
					<td>Adds a single, or multiple wads/pk3s to your server.</td>
					<td><span class="code">wad="firstwad.wad, secondwad.pk3"</span></td>
				</tr>
				<tr>
					<td><strong>Skill</strong></td>
					<td>Adjusts the skill of the server. 4 is nightmare.</td>
					<td><span class="code">skill=4</span></td>
				</tr>
				<tr>
					<td><strong>Data</strong></td>
					<td>Enables automatic loading of skulltag data (skulltag_actors and skulltag_data)</td>
					<td><span class="code">data=true</span></td>
				</tr>
				<tr>
					<td><strong>Config</strong></td>
					<td>Loads an uploaded configuration file. You can upload/view configuration files <a href="/configs">here</a>.</td>
					<td><span class="code">config="example.cfg"</span></td>
				</tr>
<!--
				<tr>
					<td><strong>Autorestart</strong></td>
					<td>If the server crashes, automatically restarts it.</td>
					<td><span class="code">autorestart=true</span></td>
				</tr>
-->
				<tr>
					<td><strong>DMFlags</strong></td>
					<td>Sets the dmflags.</td>
					<td><span class="code">dmflags=1024</span></td>
				</tr>
				<tr>
					<td><strong>DMFlags2</strong></td>
					<td>Sets the dmflags2.</td>
					<td><span class="code">dmflags2=1024</span></td>
				</tr>				<tr>
					<td><strong>DMFlags3</strong></td>
					<td>Sets the dmflags3.</td>
					<td><span class="code">dmflags3=1024</span></td>
				</tr>
				<tr>
					<td><strong>ZADMFlags</strong></td>
					<td>Will eventually replace dmflags3</td>
					<td><span class="code">zadmflags=1024</span></td>
				</tr>
				<tr>
					<td><strong>CompatFlags</strong></td>
					<td>Sets the compatflags.</td>
					<td><span class="code">compatflags=1024</span></td>
				</tr>
				<tr>
					<td><strong>CompatFlags2</strong></td>
					<td>Sets the compatflags2.</td>
					<td><span class="code">compatflags2=1024</span></td>
				</tr>
				<tr>
					<td><strong>ZACompatFlags</strong></td>
					<td>Will eventually replace compatflags2</td>
					<td><span class="code">zacompatflags=1024</span></td>
				</tr>
				<tr>
					<td><strong>Version</strong></td>
					<td>Sets the server to a different version</td>
					<td><span class="code">version=3.0</span></td>
				</tr>

			</table>
		</div>
		<div class="heading">Managing your server</div>
		<div class="content">
			After you've started the server, Painkiller should send you a private message containing the RCON password of your server. If you'd like to utilize RCON
			to change more advanced settings around, follow the <a href="/rcon/">RCON Guide</a>. If you'd like to retreive your RCON password, simply send this message to IRC:
			<span class="code">/msg Painkiller .rcon &lt;port&gt;</span>. Alternatively, check the website for the RCON/Logfile.
		</div>
	</div>
</div>

@include ('includes.footer')

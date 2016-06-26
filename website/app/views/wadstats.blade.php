@include ('includes.header')

<script>
$(document).ready(function() {
	$.getJSON( "/api/wadstats", { date: "all" }, function( data ) {
		$('#wadstats tr').not(':first').remove();
		$.each(data, function(i) {
			$('#wadstats').append('<tr><td>' + (i+1) + '</td><td>' + data[i].name + '</td><td>' + data[i].downloads + '</td></tr>');
		});
	});
	$('#time li').click(function(e) {
		e.preventDefault();
		$.getJSON( "/api/wadstats", { date: e.target.id }, function( data ) {
			$('#wadstats tr').not(':first').remove();
			$.each(data, function(i) {
				$('#wadstats').append('<tr><td>' + (i+1) + '</td><td>' + data[i].name + '</td><td>' + data[i].downloads + '</td></tr>');
			});
		});
		return false;
	});
});
</script>

<div class="main-content">
	<div class="heading">Wad Statistics</div>
	<div class="block">
		<table class="table table-bordered table-striped" style="background: #F5F5F5">
			<tr>
				<th>Total Wads</th>
				<th>WAD Archive Size</th>
				<th>WAD Downloads (Since April 15th, 2015)</th>
			</tr>
			<tr>
				<td>{{{ $count }}}</td>
				<td>{{{ $size }}}</td>
				<td>{{{ $downloads }}}</td>
			</tr>
		</table>
	</div>
	<div class="heading">Top Downloads</div>
	<div class="block">
		Notice: Download statistics are calculated starting from April 15th, 2015.
	</div>
	<ol id="time" style="margin-top: 10px;background:#EFEFEF;border-radius:0px;margin-bottom:0px" class="breadcrumb">
		<li><a id="all" href="#">All Time</a></li>
		<li><a id="month" href="#">This Month</a></li>
		<li><a id="week" href="#">This Week</a></li>
		<li><a id="day" href="#">Today</a></li>
	</ol>
	<div class="block">
		<table id="wadstats" style="background: #F5F5F5" class="table table-bordered table-striped">
			<tr>
				<th>Rank</th>
				<th>Wad</th>
				<th>Downloads</th>
			</tr>
		</table>
	</div>
	<div class="heading">Server Stats</div>
	<div class="block" style="background: rgb(240, 240, 240);">
		<center>
			<h3 style="color: black; padding: 10px;">Painkiller</h3>
			<img style="vertical-align: top; padding: 10px;" src="https://munin.csnxs.uk/munin-cgi/munin-cgi-graph/allfearthesentinel.net/painkiller/cpu-week.png"/>
			<img style="vertical-align: top; padding: 10px;" src="https://munin.csnxs.uk/munin-cgi/munin-cgi-graph/allfearthesentinel.net/painkiller/memory-week.png"/>
			<img style="vertical-align: top; padding: 10px;" src="https://munin.csnxs.uk/munin-cgi/munin-cgi-graph/allfearthesentinel.net/painkiller/fw_packets-week.png"/>
		</center>
	</div>
</div>

@include ('includes.footer')

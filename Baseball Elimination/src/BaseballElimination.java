import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.SET;

public class BaseballElimination {
	
	private final int n;
	private final Map<String,Integer> teams;
	private final String[] names;
	private final int[] w, l, r;
	private final int[][] g;

	// create a baseball division from given filename in format specified below
	public BaseballElimination(String filename)
	{
		if (filename == null)
			throw new IllegalArgumentException("Null Argument");
		
		this.n = getSize(filename);
		teams = new HashMap<String,Integer>();
		names = new String[n];
		w = new int[n];
		l = new int[n];
		r = new int[n];
		g = new int[n][n];
		
		initializeInfo(filename);
		
	}

	// number of teams
	public int numberOfTeams() 
	{
		return n;
	}

	// all teams
	public Iterable<String> teams()       
	{
		
		return teams.keySet();
	}

	// number of wins for given team
	public int wins(String team)    
	{
		if (team == null || !teams.containsKey(team))
			throw new IllegalArgumentException("Null Argument");
		
		int i = teams.get(team);
		return w[i];
	}

	// number of losses for given team
	public int losses(String team)      
	{
		if (team == null || !teams.containsKey(team))
			throw new IllegalArgumentException("Null Argument");
		
		int i = teams.get(team);
		return l[i];
	}

	// number of remaining games for given team
	public int remaining(String team)       
	{
		if (team == null || !teams.containsKey(team))
			throw new IllegalArgumentException("Null Argument");
		
		int i = teams.get(team);
		return r[i];
	}

	// number of remaining games between team1 and team2
	public int against(String team1, String team2) 
	{
		if (team1 == null || team2 == null || !teams.containsKey(team1) || !teams.containsKey(team2))
			throw new IllegalArgumentException("Null Argument");
		
		int i = teams.get(team1);
		int j = teams.get(team2);
		return g[i][j];
	}

	// is given team eliminated?
	public boolean isEliminated(String team)
	{
		if (team == null || !teams.containsKey(team))
			throw new IllegalArgumentException("Null Argument");
		
		int x = teams.get(team);
		
		//Using the easy way
		int max = w[x] + r[x];
		for (int i = 0; i < n; i++)
		{
			if (w[i] > max && i != x)
				return true;
		}
		
		FlowNetwork network = reformulateFlowNetwork(x);
		FordFulkerson ff = new FordFulkerson(network , 0 , network.V() - 1);
		
		double maxflow = 0;
		for (FlowEdge e : network.adj(0))
			maxflow += e.capacity();
		
		if (ff.value() < maxflow)
			return true;
		
		return false;
	}

	// subset R of teams that eliminates given team; null if not eliminated
	public Iterable<String> certificateOfElimination(String team) 
	{
		if (team == null || !teams.containsKey(team))
			throw new IllegalArgumentException("Null Argument");
		
		if (!isEliminated(team))
			return null;
		
		int x = teams.get(team);
		Iterable<String> result = new SET<String>();
		
		//Easy way
		int max = w[x]+r[x];
		for (int i = 0 ; i < n ; i++)
		{
			if (w[i] > max && i != x)
			{
				((SET<String>)result).add(names[i]);
				return result;
			}
		}
		
		FlowNetwork network = reformulateFlowNetwork(x);
		FordFulkerson ff = new FordFulkerson(network, 0 , network.V()-1);
		
		int vertStart = combination(n)+1;
		
		for (int i = 0; i < n ; i++)
		{
			if (i != x)
			{
				int v = vertStart+i;
				if (ff.inCut(v))
					((SET<String>)result).add(names[i]);
			}
		}
		return result;
	}
	
	private int getSize(String filename) {
		Scanner scanner;
		int n = -1;
		try {
			scanner = new Scanner(new File(filename));
			n = Integer.parseInt(scanner.nextLine());
			scanner.close();
		} catch (FileNotFoundException e) {/*Empty Catch Block*/}
		
		return n;	
	}
	
	private void initializeInfo(String filename)
	{
		Scanner scanner;
		try {
			scanner = new Scanner(new File(filename));
			int n = Integer.parseInt(scanner.nextLine());
			
			for (int i = 0; i < n ; i++)
			{
				String line = scanner.nextLine();
				line = line.trim();
				while (line.contains("  "))
					line = line.replace("  ", " ");
				
				String[] fields = line.split(" ");
				
				teams.put(fields[0], i);
				names[i] = fields[0];
				
				w[i] = Integer.parseInt(fields[1]);
				l[i] = Integer.parseInt(fields[2]);
				r[i] = Integer.parseInt(fields[3]);
				
				for (int j = 4 ; j < fields.length ; j++)
					g[i][j - 4] = Integer.parseInt(fields[j]);
			}
			scanner.close();
		} catch (FileNotFoundException e) {/*Empty Catch Block*/}
		
		
	}
	
	private FlowNetwork reformulateFlowNetwork(int x) {
		
		int vertices = 1 + combination(n) + (n) + 1;
		FlowNetwork network = new FlowNetwork(vertices);
		
		int counter = 1;
		int vertStart = combination(n) + 1;
		
		for (int i = 0; i < n-1 ; i++)
		{
			for (int j = i+1 ; j < n ; j++)
			{
				if (i != x && j != x)
				{
					FlowEdge e = new FlowEdge(0 , counter , g[i][j]);
					network.addEdge(e);
					FlowEdge e1 = new FlowEdge(counter , vertStart+i , Double.POSITIVE_INFINITY);
					FlowEdge e2 = new FlowEdge(counter , vertStart+j , Double.POSITIVE_INFINITY);
					network.addEdge(e1);
					network.addEdge(e2);
				}
				counter++;
			}
		}
		
		for (int i = 0; i < n ; i++)
		{
			if (i != x)
			{
				FlowEdge e = new FlowEdge(vertStart+i , vertices - 1 , w[x] + r[x] - w[i]);
				network.addEdge(e);
			}
		}
		
		return network;
	}
	
	private int combination(int n) {
		
		return n*(n+1) / 2;
	}

}

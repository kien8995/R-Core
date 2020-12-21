
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Spliterator;
import java.util.Stack;
import java.util.Vector;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class Main {

	// path to input/output file
	private static final String INPUT = "FigS1.txt";
	private static final String OUTPUT = "output.txt";

	// list to store edges
	private List<Edge> edgeList;
	// map to store r-core
	private Map<String, Integer> rCore;
	// map to store adjacency list
	private Map<String, Vector<String>> adjList;
	// map to store degree
	private Map<String, Integer> reachability;
	// vertex queue
	private PriorityQueue<Vertex> vertexQueue;

	private Set<String> vertexList;
	private Set<String> visited;

	private static Map<String, Set<String>> reachableList;

	public static void main(String[] args) throws Exception {
		int mb = 1024 * 1024;
		// Getting the runtime reference from system
		Runtime runtime = Runtime.getRuntime();
		System.out.println("##### Heap utilization statistics [MB] #####");

		Main main = new Main();
		main.init();
		main.readFile();
		main.loadData();
		System.out.println(main.vertexList.size());
		long start = System.currentTimeMillis();
		main.compute();
		long end = System.currentTimeMillis();
		System.out.println(end - start);
		main.writeTextFile();

		// Print used memory
		System.out.println("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);
	}

	// initialize
	public void init() {
		edgeList = new ArrayList<>();
		rCore = new HashMap<>();
		adjList = new HashMap<>();
		reachability = new HashMap<>();
		vertexQueue = new PriorityQueue<>();
		vertexList = new HashSet<>();
		visited = new HashSet<>();
		reachableList = new HashMap<>();
	}

	// read input.txt and convert edge list to adjacency list
	public void readFile() {

		Path path = Paths.get(INPUT);

		try (Stream<String> lines = Files.lines(path)) {
			Spliterator<String> lineSpliterator = lines.spliterator();
			Spliterator<Edge> edgeSpliterator = new EdgeSpliterator(lineSpliterator);

			Stream<Edge> edgeStream = StreamSupport.stream(edgeSpliterator, false);
			edgeStream.forEach(edge -> edgeList.add(edge));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// load data
	public void loadData() {
		for (Edge edge : edgeList) {
			pushMapV(adjList, edge.getStartNode(), edge.getEndNode());
			vertexList.add(edge.getStartNode());
			vertexList.add(edge.getEndNode());
		}

		for (String vertex : vertexList) {
			visited.clear();
			int n = countChildNode(vertex, vertex);
			reachability.put(vertex, n);
		}

		for (Map.Entry<String, Integer> entry : reachability.entrySet()) {
			vertexQueue.add(new Vertex(entry.getKey(), entry.getValue()));
		}
	}

	// write result to output.txt
	public void writeTextFile() throws Exception {

		Path path = Paths.get(OUTPUT);
		List<String> lines = new ArrayList<>();
		// sort map by value
		Map<String, Integer> sortedMap = MapComparator.sortByValue(rCore);
		lines.add("Node\tRCore");
		for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
			lines.add(String.format("%s\t%d", entry.getKey(), entry.getValue()));
		}

		Files.write(path, lines);
	}

	// push value to map
	public void pushMapV(Map<String, Vector<String>> adjList, String start, String end) {
		if (!adjList.containsKey(start)) {
			adjList.put(start, new Vector<>());
		}
		adjList.get(start).add(end);
	}

	public void pushMapS(Map<String, Set<String>> adjList, String start, String end) {
		if (!adjList.containsKey(start)) {
			adjList.put(start, new HashSet<>());
		}
		adjList.get(start).add(end);
	}

	public int countChildNode(String node, String source) {
		int count = 0;
		visited.add(node);
		if (adjList.get(node) != null) {
			for (String vertex : adjList.get(node)) {
				if (!visited.contains(vertex)) {
					if (adjList.get(vertex) != null && adjList.get(vertex).size() > 0) {
						count = count + countChildNode(vertex, source);
					}
					count = count + 1;
					visited.add(vertex);
					pushMapS(reachableList, source, vertex);
				}
			}
		}
		return count;
	}

	public int countChildNodeSe(String node) {
		Stack<String> s = new Stack<>();

		int count = 0;

		s.push(node);

		while (!s.isEmpty()) {
			String current = s.pop();

			if (visited.contains(current)) {
				continue;
			}

			visited.add(current);

			if (adjList.get(current) != null) {
				for (String vertex : adjList.get(current)) {
					s.push(vertex);
					if (!visited.contains(vertex)) {
						count = count + 1;
						pushMapS(reachableList, node, vertex);
					}
				}
			}
		}

		return count;
	}

	// compute
	public void compute() {
		int r = 0;
		// BFS traverse
		while (!vertexQueue.isEmpty()) {
			Vertex current = vertexQueue.poll();
			if (reachability.get(current.getVertex()) < current.getDegree()) {
				continue;
			}

			r = Math.max(r, reachability.get(current.getVertex()));

			rCore.put(current.getVertex(), Integer.valueOf(r));
			// sequentially
			if (adjList.get(current.getVertex()) != null && reachability.get(current.getVertex()) > 0) {

				for (String vertex : adjList.get(current.getVertex())) {
					if (!rCore.containsKey(vertex) && reachability.get(vertex) != null) {
						reachability.put(vertex, reachability.get(vertex) - 1);

						for (Map.Entry<String, Set<String>> entry : reachableList.entrySet()) {
							if (entry.getValue().contains(vertex)) {
								reachability.put(entry.getKey(), reachability.get(entry.getKey()) - 1);
							}

						}
						vertexQueue.add(new Vertex(vertex, reachability.get(vertex)));
					}
				}

			} else if (reachability.get(current.getVertex()) == 0) {
				for (Map.Entry<String, Set<String>> entry : reachableList.entrySet()) {
					if (entry.getValue().contains(current.getVertex())) {
						reachability.put(entry.getKey(), reachability.get(entry.getKey()) - 1);
						vertexQueue.add(new Vertex(entry.getKey(), reachability.get(entry.getKey())));
					}
				}
			}
		}

		System.out.println("R-Core: " + r);
	}

	public void writeXLSFile() throws Exception {

		// sort map by value
		Map<String, Integer> sortedMap = MapComparator.sortByValue(rCore);

		// name of excel file
		String excelFileName = OUTPUT;

		// name of sheet
		String sheetName = "Sheet1";

		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = wb.createSheet(sheetName);
		HSSFRow row;
		HSSFCell cell;

		// header
		row = sheet.createRow(0);
		cell = row.createCell(0);
		cell.setCellValue("Node");
		cell = row.createCell(1);
		cell.setCellValue("Rank");

		int index = 1;
		for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
			row = sheet.createRow(index++);

			cell = row.createCell(0);
			cell.setCellValue(String.format("%s", entry.getKey()));

			cell = row.createCell(1);
			cell.setCellValue(String.format("%d", entry.getValue()));
		}

		FileOutputStream fileOut = new FileOutputStream(excelFileName);

		// write this workbook to an Outputstream.
		wb.write(fileOut);
		fileOut.flush();
		fileOut.close();
	}
}

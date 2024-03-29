
import java.util.Spliterator;
import java.util.function.Consumer;

public class EdgeSpliterator implements Spliterator<Edge> {

	private final Spliterator<String> lineSpliterator;
	private String line;

	public EdgeSpliterator(Spliterator<String> lineSpliterator) {
		this.lineSpliterator = lineSpliterator;
	}

	@Override
	public boolean tryAdvance(Consumer<? super Edge> action) {
		if (this.lineSpliterator.tryAdvance(line -> this.line = line)) {
			String[] line = this.line.split("\\s+");
			//System.out.println(line[0] + "-" + line[1] + "-" + line[2]);
			//Edge edge = new Edge(line[0], line[1]);
			Edge edge = new Edge(line[0], line[1], Integer.valueOf(line[2]));
			action.accept(edge);
			return true;
		}
		return false;
	}

	@Override
	public Spliterator<Edge> trySplit() {
		return null;
	}

	@Override
	public long estimateSize() {
		return lineSpliterator.estimateSize();
	}

	@Override
	public int characteristics() {
		return lineSpliterator.characteristics();
	}
}

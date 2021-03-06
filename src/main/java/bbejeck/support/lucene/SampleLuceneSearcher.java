package bbejeck.support.lucene;

import bbejeck.support.BaseSample;

import com.google.common.util.concurrent.ListenableFuture;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static java.lang.System.out;
/**
 * Created by IntelliJ IDEA.
 * User: bbejeck
 * Date: 11/20/11
 * Time: 2:06 PM
 */
public class SampleLuceneSearcher extends BaseSample {

    private IndexSearcher searcher;
    private final int MAX_RESULTS = 1000;

    public SampleLuceneSearcher(RAMDirectory ramDirectory) {
        try {
            searcher = new IndexSearcher(IndexReader.open(ramDirectory, true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> search(String query) throws Exception {
        List<String> results = new ArrayList<String>();
        QueryParser queryParser = new QueryParser(Version.LUCENE_35, null, new StandardAnalyzer(Version.LUCENE_35));
        Query q = queryParser.parse(query);
        TopDocs topDocs = searcher.search(q, MAX_RESULTS);
        for (ScoreDoc sd : topDocs.scoreDocs) {
            Document document = searcher.doc(sd.doc);
            results.add(document.get("id"));
        }
        return results;
    }

    public ListenableFuture<List<String>> searchAsync(final String query)  {
        return executorService.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
				out.println(Thread.currentThread().getName());

                return search(query);
            }
        });
    }

}

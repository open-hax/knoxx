import { useState, useEffect } from "react";
import * as api from "../lib/nextApi";

export default function VectorsPage() {
  const [health, setHealth] = useState<any>(null);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<any[]>([]);

  const fetchHealth = async () => {
    try {
      const res = await api.knoxxHealth();
      setHealth(res);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  const handleSearch = () => {
    setResults([{ id: 1, score: 0.98, text: "Mock search result 1" }]);
  };

  return (
    <div className="p-4 max-w-4xl mx-auto space-y-4 text-slate-100">
      <h1 className="text-2xl font-bold">Vector Store / RAG Internals</h1>
      
      <div className="grid grid-cols-2 gap-4">
        <div className="border border-slate-700 p-4 rounded bg-slate-900">
          <h2 className="font-bold mb-2">Collection Health</h2>
          <pre className="text-sm text-slate-300">{JSON.stringify(health, null, 2)}</pre>
        </div>
        
        <div className="border border-slate-700 p-4 rounded bg-slate-900">
          <h2 className="font-bold mb-2">Hybrid Migration Helper</h2>
          <p className="text-sm mb-4 text-slate-300">Migrate your dense index to hybrid search with sparse embeddings.</p>
          <button className="bg-orange-500 text-white px-4 py-2 rounded">Trigger Migration</button>
        </div>
      </div>

      <div className="border border-slate-700 bg-slate-900 p-4 rounded mt-8">
        <h2 className="font-bold mb-4">Search Debug Tool</h2>
        <div className="flex space-x-2 mb-4">
          <input 
            type="text" 
            value={query} 
            onChange={(e) => setQuery(e.target.value)}
            className="field-input flex-1"
            placeholder="Search query..."
          />
          <button onClick={handleSearch} className="bg-blue-500 text-white px-4 py-2 rounded">Search</button>
        </div>
        
        <div className="space-y-2">
          {results.map((r, i) => (
            <div key={i} className="p-2 bg-slate-800 rounded text-sm text-slate-200 border border-slate-700">
              <strong>{r.score.toFixed(3)}:</strong> {r.text}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

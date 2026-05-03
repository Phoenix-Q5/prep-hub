import { useState, useCallback, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Search, Loader2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useDebounce } from "@/hooks/useDebounce";
import { searchApi } from "@/api/endpoints";
import type { SearchHit } from "@/types";

export function SearchBar() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchHit[]>([]);
  const [loading, setLoading] = useState(false);
  const [open, setOpen] = useState(false);
  const debounced = useDebounce(query, 150);
  const ref = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (debounced.length < 2) { setResults([]); return; }
    let cancelled = false;
    setLoading(true);
    searchApi.typeahead(debounced, undefined, 8).then((hits) => {
      if (!cancelled) { setResults(hits); setOpen(true); }
    }).finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [debounced]);

  // Close on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const handleSelect = useCallback((id: string) => {
    setOpen(false);
    setQuery("");
    navigate(`/questions/${id}`);
  }, [navigate]);

  const difficultyColor: Record<string, string> = {
    EASY: "bg-green-100 text-green-800",
    MEDIUM: "bg-yellow-100 text-yellow-800",
    HARD: "bg-red-100 text-red-800",
  };

  return (
    <div ref={ref} className="relative w-full max-w-lg">
      <div className="relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => results.length > 0 && setOpen(true)}
          placeholder="Search topics..."
          className="pl-9 pr-8"
        />
        {loading && <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 animate-spin text-muted-foreground" />}
      </div>

      {open && results.length > 0 && (
        <div className="absolute top-full left-0 right-0 z-50 mt-1 rounded-md border bg-background shadow-lg max-h-80 overflow-y-auto">
          {results.map((hit) => (
            <button
              key={hit.id}
              onClick={() => handleSelect(hit.id)}
              className="flex w-full items-start gap-3 px-3 py-2.5 text-left hover:bg-accent transition-colors border-b last:border-b-0"
            >
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{hit.title}</p>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-xs text-muted-foreground">{hit.topicName}</span>
                  {hit.difficulty && (
                    <span className={`text-[10px] px-1.5 py-0.5 rounded ${difficultyColor[hit.difficulty] || ""}`}>
                      {hit.difficulty}
                    </span>
                  )}
                  <span className="text-xs text-muted-foreground">{hit.likeCount} likes</span>
                </div>
              </div>
            </button>
          ))}
        </div>
      )}

      {open && debounced.length >= 2 && results.length === 0 && !loading && (
        <div className="absolute top-full left-0 right-0 z-50 mt-1 rounded-md border bg-background shadow-lg p-4 text-center text-sm text-muted-foreground">
          No results for "{debounced}"
        </div>
      )}
    </div>
  );
}

import 'package:flutter/material.dart';
import 'core/theme/gemini_theme.dart';

void main() {
  runApp(const EngBuddyV2App());
}

class EngBuddyV2App extends StatelessWidget {
  const EngBuddyV2App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'EngBuddy v2.0',
      debugShowCheckedModeBanner: false,
      theme: GeminiTheme.light(),
      darkTheme: GeminiTheme.dark(),
      themeMode: ThemeMode.dark,
      home: const LiquidGlassHomeShell(),
    );
  }
}

class LiquidGlassHomeShell extends StatefulWidget {
  const LiquidGlassHomeShell({super.key});

  @override
  State<LiquidGlassHomeShell> createState() => _LiquidGlassHomeShellState();
}

class _LiquidGlassHomeShellState extends State<LiquidGlassHomeShell> {
  int _selectedIndex = 0;
  int _attended = 25;
  int _total = 32;

  @override
  Widget build(BuildContext context) {
    final double pct = (_attended / _total) * 100.0;
    final int safeBunks = ((_attended - (0.75 * _total)) / 0.75).floor().clamp(0, 99);

    return Scaffold(
      backgroundColor: const Color(0xFF0B0C0E),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'EngBuddy v2.0',
                    style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.06),
                      borderRadius: BorderRadius.circular(50),
                      border: Border.all(color: Colors.white.withValues(alpha: 0.14)),
                    ),
                    child: const Text(
                      '23CS10042',
                      style: TextStyle(
                        fontFamily: 'JetBrainsMono',
                        color: Color(0xFFFBBC04),
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              // Primary Card 1: Liquid Glass Bunk Horizon
              Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.04),
                  borderRadius: BorderRadius.circular(28),
                  border: Border.all(color: Colors.white.withValues(alpha: 0.09)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${pct.toStringAsFixed(0)}%  •  +$safeBunks BUNKS LEFT',
                      style: const TextStyle(
                        fontSize: 38,
                        fontWeight: FontWeight.w900,
                        color: Color(0xFF34A853),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'You can skip the next $safeBunks classes and still stay at 75%.',
                      style: const TextStyle(fontSize: 16),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              // Primary Card 2: Interactive Live Class Notification Prompt
              Container(
                padding: const EdgeInsets.all(22),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.04),
                  borderRadius: BorderRadius.circular(24),
                  border: Border.all(color: const Color(0xFF4285F4), width: 1.5),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Thermodynamics (ME-204) is going on',
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Should I mark your attendance for this class? (Current: ${pct.toStringAsFixed(0)}% • Safe to bunk: $safeBunks)',
                    ),
                    const SizedBox(height: 14),
                    Wrap(
                      spacing: 8,
                      children: [
                        ElevatedButton(
                          onPressed: () => setState(() {
                            _attended++;
                            _total++;
                          }),
                          child: const Text('Yes, Attending'),
                        ),
                        OutlinedButton(
                          onPressed: () => setState(() {
                            _total++;
                          }),
                          child: const Text('No, Bunking'),
                        ),
                        TextButton(
                          onPressed: () {},
                          child: const Text('Class Cancelled'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: Padding(
        padding: const EdgeInsets.all(20),
        child: NavigationBar(
          selectedIndex: _selectedIndex,
          onDestinationSelected: (idx) => setState(() => _selectedIndex = idx),
          destinations: const [
            NavigationDestination(icon: Icon(Icons.home_outlined), label: 'Home'),
            NavigationDestination(icon: Icon(Icons.folder_special_outlined), label: 'Vault'),
            NavigationDestination(icon: Icon(Icons.camera_alt_outlined), label: 'Scan'),
            NavigationDestination(icon: Icon(Icons.event_available_outlined), label: 'Bunks'),
            NavigationDestination(icon: Icon(Icons.auto_awesome_outlined), label: 'AI Buddy'),
          ],
        ),
      ),
    );
  }
}
